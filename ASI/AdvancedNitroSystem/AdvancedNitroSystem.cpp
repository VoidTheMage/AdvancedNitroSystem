#include "plugin.h"
#include "CStreaming.h"
#include "CMessages.h"
#include "../third_party/IniReader/IniReader.h"
#include "CEntryExitManager.h"
#include "CHud.h"
#include "CMenuManager.h"
#include "CReplay.h"
#include "extensions/ScriptCommands.h"

using namespace plugin;

bool disableOnSAMP;
int barFillDirection;
bool drawBarBorder;
bool bilinearOffset;
CRGBA rgb;
float offsetX;
float offsetY;
float offsetXNR;
float offsetYNR;
float barLenght;
float barThickness;
int borderThickness;
bool consumableMode;

float currentNos;
float drawThisFrame;
bool reloadNosThisFrame;
int refillNosAddress;

float scaleX;
float scaleY;
float posX;
float posY;

extern "C"
{
    __declspec(dllexport) int GetNitroValue()
    {
        return *(int*)&currentNos; //Raw bits
    }

    __declspec(dllexport) void SetNitroValue(float value)
    {
        currentNos = value * 100;
    }

    __declspec(dllexport) void DrawBar()
    {
        drawThisFrame = true;
    }

    __declspec(dllexport) void ReloadNos()
    {
        reloadNosThisFrame = true;
    }

    __declspec(dllexport) void Subscribe(int value)
    {
        refillNosAddress = value;
    }
}

static ThiscallEvent <AddressList<0x6D31E3, H_CALL>, PRIORITY_BEFORE, ArgPick2N<CAutomobile*, 0, char, 1>, void(CAutomobile*, char)> NitrousControl;

void NitrousControlHook(CAutomobile* x, char setBoosts);
void __fastcall NitrousControlHookNPCFix(CAutomobile* x, void* edx, char setBoosts);
int __fastcall GetUpgradeHook(CVehicle* veh, void* edx, int type);
void DrawProgressBarOnScreen(float x, float y, float width, float height, float progress, CRGBA const& color);
void DrawProgressBar();
bool IsRadarVisible();
void PatchVehFireButtonGInput();
void CheckReloadNos();
void __cdecl NitroCheatAndTaxiHandler(CAutomobile* automobile);
void NitroCheatAndTaxiHandler_thunk();
void LoadIniValues();
int Ret0();

void NitrousControlHook(CAutomobile* x, char setBoosts)
{
    if(refillNosAddress && x->m_pDriver == FindPlayerPed())
    {
        patch::SetInt(refillNosAddress, 1);
    }
}

void __fastcall NitrousControlHookNPCFix(CAutomobile* x, void* edx, char setBoosts)
{
    if(x->m_pDriver)
    {
        if(x->m_pDriver->IsPlayer()) //Fixes NPCS using nitro
        {
            x->NitrousControl(setBoosts); //Don't affect bikes
        }
    }
    else
    {
        x->NitrousControl(setBoosts); //Don't affect bikes
    }
}

int __fastcall GetUpgradeHook(CVehicle* veh, void* edx, int type)
{
    if(type == 15)
    {
        for(int i = 0; i < 15; ++i)
        {
            if(veh->m_anUpgrades[i] == 1009 || veh->m_anUpgrades[i] == 1008 || veh->m_anUpgrades[i] == 1010)
            {
                return veh->m_anUpgrades[i];
            }
        }

        return -1;
    }

    return veh->GetUpgrade(type);
}

void DrawProgressBarOnScreen(float x, float y, float width, float height, float progress, CRGBA const& color)
{
    if(progress > 100.0f)
    {
        progress = 100.0f;
    }

    //Total background area
    float left = x;
    float top = y;
    float right = x + width;
    float bottom = y + height;

    //Defines the main dimensions according to fill direction
    float mainDim = 0;

    if(barFillDirection == 0 || barFillDirection == 2) //Horizontal
    {
        mainDim = width;
    }
    else if(barFillDirection == 1 || barFillDirection == 3) //Vertical
    {
        mainDim = height;
    }

    float progressDim = mainDim * (progress / 100.f);
    float fillLeft, fillTop, fillRight, fillBottom;

    switch(barFillDirection)
    {
    case 0: //Left to Right
        fillLeft = x;
        fillTop = y;
        fillRight = x + progressDim;
        fillBottom = y + height;
        break;
    case 1: //Bottom to Top
        fillLeft = x;
        fillTop = y + height - progressDim;
        fillRight = x + width;
        fillBottom = y + height;
        break;
    case 2: //Right to Left
        fillLeft = x + width - progressDim;
        fillTop = y;
        fillRight = x + width;
        fillBottom = y + height;
        break;
    case 3: //Top to Bottom
        fillLeft = x;
        fillTop = y;
        fillRight = x + width;
        fillBottom = y + progressDim;
        break;
    default: //Left to Right
        fillLeft = x;
        fillTop = y;
        fillRight = x + progressDim;
        fillBottom = y + height;
        break;
    }

    if(bilinearOffset)
    {
        if(progress < 100.0f) //Draws empty area
        {
            CSprite2d::DrawRect(
                CRect(floor(left * scaleX) - 0.5f, floor(top * scaleY) - 0.5f,
                    floor(right * scaleX) - 0.5f, floor(bottom * scaleY) - 0.5f),
                CRGBA(color.r, color.g, color.b, color.a / 2));
        }

        if(progress > 0.0f) //Draws filled up area
        {
            CSprite2d::DrawRect(
                CRect(floor(fillLeft * scaleX) - 0.5f, floor(fillTop * scaleY) - 0.5f,
                    floor(fillRight * scaleX) - 0.5f, floor(fillBottom * scaleY) - 0.5f),
                color);
        }
    }
    else
    {
        if(progress < 100.0f) //Draws empty area
        {
            CSprite2d::DrawRect(
                CRect((float)(left * scaleX), (float)(top * scaleY),
                    (float)(right * scaleX), (float)(bottom * scaleY)),
                CRGBA(color.r, color.g, color.b, color.a / 2));
        }

        if(progress > 0.0f) //Draws filled up area
        {
            CSprite2d::DrawRect(
                CRect((float)(fillLeft * scaleX), (float)(fillTop * scaleY),
                    (float)(fillRight * scaleX), (float)(fillBottom * scaleY)),
                color);
        }
    }
}

void DrawProgressBar()
{
    if(!drawThisFrame) return;

    float x = offsetX;
    float y = offsetY;

    if(!IsRadarVisible())
    {
        x = offsetXNR;
        y = offsetYNR;
    }

    float width = barLenght;
    float height = barThickness;

    if(barFillDirection == 1 || barFillDirection == 3)
    {
        float swap = width;
        width = height;
        height = swap;
    }

    scaleX = (float)RsGlobal.maximumWidth / 1920.0f;
    scaleY = (float)RsGlobal.maximumHeight / 1080.0f;

    float resX = (float)RsGlobal.maximumWidth;
    float resY = (float)RsGlobal.maximumHeight;
    resY *= 1.33333333f;
    resX /= resY;

    posX = x / resX;
    posY = y / 1.07142857f;

    if(drawBarBorder)
    {
        if(bilinearOffset)
        {
            CSprite2d::DrawRect(CRect(
                floor((posX)*scaleX) - 0.5f - borderThickness,
                floor((posY)*scaleY) - 0.5f - borderThickness,
                floor((posX + width) * scaleX) - 0.5f + borderThickness,
                floor((posY + height) * scaleY) - 0.5f + borderThickness
            ), CRGBA(0, 0, 0, 255));
        }
        else
        {
            CSprite2d::DrawRect(CRect(
                (float)((posX - borderThickness) * scaleX),
                (float)((posY - borderThickness) * scaleY),
                (float)((posX + width + borderThickness) * scaleX),
                (float)((posY + height + borderThickness) * scaleY)
            ), CRGBA(0, 0, 0, 255));
        }
    }
    DrawProgressBarOnScreen(posX, posY, width, height, currentNos, rgb);

    drawThisFrame = false;
}

bool IsRadarVisible()
{
    return CEntryExitManager::ms_exitEnterState != 1
        && CEntryExitManager::ms_exitEnterState != 2
        && !CHud::bScriptDontDisplayRadar
        && FrontEndMenuManager.m_nPrefsRadarMode != 2
        && (CHud::m_ItemToFlash != 8 || CTimer::m_FrameCounter & 8)
        && CReplay::Mode != 1
        && !CWeapon::ms_bTakePhoto
        && (*(unsigned int*)0xB6EC40 != CTimer::m_FrameCounter);
}

void PatchVehFireButtonGInput()
{
    if(uintptr_t base = reinterpret_cast<uintptr_t>(GetModuleHandleA("GInputSA.asi")))
    {
        CVehicle* veh = FindPlayerVehicle(0, false);
        CPad* pad = CPad::GetPad(0);

        if(Command<Commands::IS_PC_USING_JOYPAD>(0) && pad->Mode == 0 && veh)
        {
            if(veh->m_nVehicleSubClass == 10)
            {
                patch::SetShort(base + 0x3314, 0x22);
            }
            else
            {
                patch::SetShort(base + 0x3314, 0x08);
            }
        }
        else
        {
            patch::SetShort(base + 0x3314, 0x22);
        }
    }
}

void CheckReloadNos()
{
    if(reloadNosThisFrame)
    {
        LoadIniValues();
    }
    reloadNosThisFrame = false;
}

void __cdecl NitroCheatAndTaxiHandler(CAutomobile* automobile)
{
    DWORD flags = *(DWORD*)((uintptr_t)automobile + 0x38C);
    if((flags & 0x80000) == 0)
    {
        CStreaming::RequestModel(1010, eStreamingFlags::PRIORITY_REQUEST);
        CStreaming::LoadAllRequestedModels(true);
        automobile->AddVehicleUpgrade(1010);
        CStreaming::SetModelIsDeletable(1010);
    }
}

__declspec(naked) void NitroCheatAndTaxiHandler_thunk()
{
    _asm
    {
        push esi
        call NitroCheatAndTaxiHandler
        add  esp, 4
        ret
    }
}

void LoadIniValues()
{
    CIniReader ini("AdvancedNitroSystem.ini");
    disableOnSAMP = ini.ReadInteger("configs", "DisableOnSAMP", 1);
    barFillDirection = ini.ReadInteger("configs", "BarFillDirection", 0);
    drawBarBorder = ini.ReadInteger("configs", "DrawBarBorder", 0);
    rgb = CRGBA(ini.ReadInteger("configs", "BarR", 0), ini.ReadInteger("configs", "BarG", 0), ini.ReadInteger("configs", "BarB", 0), 255);
    offsetX = ini.ReadFloat("configs", "OffsetX", 0.0);
    offsetY = ini.ReadFloat("configs", "OffsetY", 0.0);
    offsetXNR = ini.ReadFloat("configs", "OffsetXNoRadar", 0.0);
    offsetYNR = ini.ReadFloat("configs", "OffsetYNoRadar", 0.0);
    barLenght = ini.ReadFloat("configs", "BarLength", 0.0);
    barThickness = ini.ReadFloat("configs", "BarThickness", 0.0);
    borderThickness = ini.ReadInteger("configs", "BorderThickness", 0);
    bilinearOffset = ini.ReadInteger("configs", "BilinearOffset", 0);

    if(!bilinearOffset)
    {
        borderThickness++;
        barLenght--;
        barThickness--;
    }

    consumableMode = ini.ReadInteger("configs", "ConsumableMode", 0); //If changed after first read can cause issues
}

int Ret0()
{
    return 0;
}

class AdvancedNitroSystem
{
public:
    AdvancedNitroSystem()
    {
        LoadIniValues();
        
        if(GetModuleHandleA("SAMP.dll") && disableOnSAMP) return;        

        plugin::Events::drawHudEvent.after += DrawProgressBar;
        plugin::Events::processScriptsEvent.after += PatchVehFireButtonGInput;
        plugin::Events::processScriptsEvent.after += CheckReloadNos;

        patch::RedirectCall(0x478462, GetUpgradeHook); //Fixes the value returned by the command for vehicles without a nitro dummy

        patch::RedirectCall(0x6A3F66, Ret0);
        patch::RedirectCall(0x6A3F76, Ret0);
        patch::RedirectCall(0x6A3F85, Ret0);
        patch::RedirectCall(0x6A3F94, Ret0);

        if(consumableMode)
        {
            NitrousControl.before += [](CAutomobile* x, char setBoosts)
            {
                NitrousControlHook(x, setBoosts);
            };
        }

        patch::RedirectCall(0x6B2038, NitrousControlHookNPCFix);

        patch::Nop(0x6A3FB9, 2); //Disable nitro amount decrease

        patch::Nop(0x6B199C, 6); //Allows NPCS taxis to get nitro

        patch::Nop(0x6B19CC, 6);
        patch::Nop(0x6B19D2, 7);
        injector::MakeCALL(0x6B19CC, NitroCheatAndTaxiHandler_thunk, true);
	};
} AdvancedNitroSystemPlugin;
