#include <plugin.h> // Plugin-SDK version 1002 from 2025-12-09 23:18:09
#include <CMessages.h>
#include <CBike.h>
#include <CModelInfo.h>
#include <FxManager_c.h>
#include <CStreaming.h>
#include "../third_party/IniReader/IniReader.h"

using namespace plugin;

bool disableOnSAMP;
bool useBikeNitro;

ThiscallEvent <AddressList<0x6B8953, H_CALL>, PRIORITY_BEFORE, ArgPickN<CBike*, 0>, void(CBike*)> CBikeDestructor;
ThiscallEvent <AddressList<0x6B9271, H_CALL>, PRIORITY_BEFORE, ArgPickN<CVehicle*, 0>, void(CVehicle*)> GetColModel;
ThiscallEvent <AddressList<0x6B9B3B, H_CALL>, PRIORITY_BEFORE, ArgPickN<CPhysical*, 0>, void(CPhysical*)> SkipPhysics;
ThiscallEvent <AddressList<0x501F9B, H_CALL>, PRIORITY_BEFORE, ArgPick2N<CAEVehicleAudioEntity*, 0, cVehicleParams*, 1>, void(CAEVehicleAudioEntity*, cVehicleParams*)> ProcessVehicleRoadNoise;
ThiscallEvent <AddressList<0x6BF768, H_CALL>, PRIORITY_BEFORE, ArgPickN<CBike*, 0>, void(CBike*)> SetupSuspensionLines;

void NitroCheatHandler(CAutomobile* automobile);
void GetColModelHook(CEntity* _this);
void SkipPhysicsHook(CPhysical* _this);
float __fastcall CalculateDriveAccelerationHook(cTransmission* _this, void* edx, float* pGasPedal, unsigned __int8* pbCurrentGear, float* pGearChangeCount, float* pSpeed, float* a6, float* a7, char bAllWheelsOnGround, char handlingType);
void ProcessVehicleRoadNoiseHook(CAEVehicleAudioEntity* _this, cVehicleParams* a2);
void SetupBikeCarmods(CBike* bike);
void SetupSuspensionLinesHook(CBike* _this);
void CBikeDestructorHook(CBike* _this);
bool GetWaterLevel(float x, float y, float z, float* pWaterZ, bool bForceResult, CVector* pNormal);
CVector TransformPoint(CMatrix* mat, CVector pos);
void __fastcall DoNitroEffectHook(CAutomobile* automobile, float power);
CBike* FindBikeFromMatrix(RwMatrix* mat);
void __fastcall GetCompositeMatrixHook(FxSystem_c* _this, void* edx, RwMatrixTag* out);
void __stdcall RemoveNitroUpgrades(CBike* bike);
void AllowBikesToAddNitro_thunk();
void AllowBikesToRemoveNitro_thunk();
void LoadIniValues();
void AddCBikeProcessControlHooks();
void FixDesync(CVehicle* veh);

void NitroCheatHandler(CAutomobile* automobile)
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

void GetColModelHook(CEntity* _this)
{
	CAutomobile* automobile = reinterpret_cast<CAutomobile*>(_this);

	if(automobile->m_nVehicleSubClass != 9) return;

	DWORD flags = *(DWORD*)((uintptr_t)automobile + 0x38C);

	if(useBikeNitro)
	{
		if(patch::GetChar(0x969165)) //All cars have nitro
		{
			NitroCheatHandler(automobile);
		}
	}

	if(flags & 0x80000)
	{
		if(automobile->m_pDriver)
		{
			if(automobile->m_pDriver->IsPlayer()) //Fixes NPCS using nitro
			{
				automobile->NitrousControl(0);
			}
		}
		else
		{
			automobile->NitrousControl(0);
		}
	}
	else
	{
		FxSystem_c* fx0 = reinterpret_cast<FxSystem_c*>(automobile->pNitroParticle[0]);
		if(fx0)
		{
			fx0->Kill();
			automobile->pNitroParticle[0] = 0;
		}
		FxSystem_c* fx1 = reinterpret_cast<FxSystem_c*>(automobile->pNitroParticle[1]);
		if(fx1)
		{
			fx1->Kill();
			automobile->pNitroParticle[1] = 0;
		}
	}
}

void SkipPhysicsHook(CPhysical* _this)
{
	CAutomobile* automobile = reinterpret_cast<CAutomobile*>(_this);

	if(automobile->m_nVehicleSubClass != 9) return;

	automobile->m_fNitroValue = 1.0;
}

float __fastcall CalculateDriveAccelerationHook(cTransmission* _this, void* edx, float* pGasPedal, unsigned __int8* pbCurrentGear, float* pGearChangeCount, float* pSpeed, float* a6, float* a7, char bAllWheelsOnGround, char handlingType)
{
	CAutomobile* automobile = reinterpret_cast<CAutomobile*>(pbCurrentGear - 0x4B4);

	if(automobile->m_nVehicleSubClass != 9) return _this->CalculateDriveAcceleration(*pGasPedal, *pbCurrentGear, *pGearChangeCount, *pSpeed, *a6, *a7, bAllWheelsOnGround, handlingType);

	DWORD flags = *(DWORD*)((uintptr_t)automobile + 0x38C);
	if((flags & 0x80000) && automobile->m_fNitroValue < 0.0)
	{
		handlingType = 2;
	}

	return _this->CalculateDriveAcceleration(*pGasPedal, *pbCurrentGear, *pGearChangeCount, *pSpeed, *a6, *a7, bAllWheelsOnGround, handlingType);
}

void ProcessVehicleRoadNoiseHook(CAEVehicleAudioEntity* _this, cVehicleParams* a2)
{
	plugin::CallMethod<0x4FB070, CAEVehicleAudioEntity*, cVehicleParams*>(_this, a2); //ProcessNitro
}

void SetupBikeCarmods(CBike* bike)
{
	CVehicleModelInfo* vehModelInfo = (CVehicleModelInfo*)CModelInfo::GetModelInfo(bike->m_nModelIndex);

	if(!vehModelInfo) return;

	bool hasNitro2X = false;
	bool hasNitro5X = false;
	bool hasNitro10X = false;

	for(int i = 0; i < 18; ++i)
	{
		int model = vehModelInfo->m_anUpgrades[i];
		if(model == -1)
		{
			for (; i < 18; ++i)
			{
				if(!hasNitro2X)
				{
					vehModelInfo->m_anUpgrades[i] = 1009;
					hasNitro2X = true;
				}
				else if(!hasNitro5X)
				{
					vehModelInfo->m_anUpgrades[i] = 1008;
					hasNitro5X = true;
				}
				else if(!hasNitro10X)
				{
					vehModelInfo->m_anUpgrades[i] = 1010;
					hasNitro10X = true;
				}
				else
				{
					break;
				}
			}
			break;
		}
		else
		{
			if(model == 1009) hasNitro2X = true;
			else if(model == 1008) hasNitro5X = true;
			else if(model == 1010) hasNitro10X = true;
		}
	}
}

void SetupSuspensionLinesHook(CBike* _this)
{
	CAutomobile* automobile = reinterpret_cast<CAutomobile*>(_this);

	if(automobile->m_nVehicleSubClass != 9) return;

	automobile->m_fNitroValue = 1.0f;

	automobile->pNitroParticle[0] = 0;
	automobile->pNitroParticle[1] = 0;

	if(useBikeNitro)
	{
		SetupBikeCarmods(_this);
	}
}

void CBikeDestructorHook(CBike* _this)
{
	CAutomobile* automobile = reinterpret_cast<CAutomobile*>(_this);

	FxSystem_c* fx0 = reinterpret_cast<FxSystem_c*>(automobile->pNitroParticle[0]);
	if(fx0)
	{
		fx0->Kill();
		automobile->pNitroParticle[0] = 0;
	}
	FxSystem_c* fx1 = reinterpret_cast<FxSystem_c*>(automobile->pNitroParticle[1]);
	if(fx1)
	{
		fx1->Kill();
		automobile->pNitroParticle[1] = 0;
	}
}

bool GetWaterLevel(float x, float y, float z, float* pWaterZ, bool bForceResult, CVector* pNormal)
{
	return plugin::CallAndReturnDynGlobal<bool>(0x6EB690, x, y, z, pWaterZ, bForceResult, pNormal);
}

CVector TransformPoint(CMatrix* mat, CVector pos)
{
	CVector point;

	point.x = mat->pos.x + pos.x * mat->right.x + pos.y * mat->up.x + pos.z * mat->at.x;
	point.y = mat->pos.y + pos.x * mat->right.y + pos.y * mat->up.y + pos.z * mat->at.y;
	point.z = mat->pos.z + pos.x * mat->right.z + pos.y * mat->up.z + pos.z * mat->at.z;

	return point;
}

void __fastcall DoNitroEffectHook(CAutomobile* automobile, float power)
{
	if(automobile->m_nVehicleSubClass != 9) return automobile->DoNitroEffect(power);

	CBike* bike = reinterpret_cast<CBike*>(automobile);
	CPhysical* physical = reinterpret_cast<CPhysical*>(automobile);
	int modelId = automobile->m_nModelIndex;
	CVehicleModelInfo* vehModelInfo = (CVehicleModelInfo*)CModelInfo::GetModelInfo(modelId);
	bike->CalculateLeanMatrix();
	CMatrix leanMat = bike->m_mLeanMatrix;

	CVector firstExhaustPos = vehModelInfo->m_pVehicleStruct->m_avDummyPos[6];
	CVector secondExhaustPos = firstExhaustPos;
	bool doubleExhaust = automobile->m_pHandlingData->m_bDoubleExhaust;

	bool firstExhaustUnderWater = false;
	bool secondExhaustUnderWater = false;
	float level = 0;

	if(modelId == MODEL_FCR900)
	{
		if(automobile->m_anExtras[0] == 1 || automobile->m_anExtras[0] == 2)
		{
			doubleExhaust = true;
		}
	}
	else if(modelId == MODEL_BF400)
	{
		if(automobile->m_anExtras[0] == 2)
		{
			doubleExhaust = true;
		}
	}

	if(physical->bTouchingWater)
	{
		CVector point = TransformPoint(&leanMat, firstExhaustPos);

		if(GetWaterLevel(point.x, point.y, point.z, &level, true, nullptr))
		{
			if(level >= point.z)
			{
				firstExhaustUnderWater = true;
			}
		}
	}

	if(doubleExhaust)
	{
		if(modelId == MODEL_NRG500 && (!automobile->m_anExtras[0] || automobile->m_anExtras[0] == 1))
		{
			secondExhaustPos = vehModelInfo->m_pVehicleStruct->m_avDummyPos[11];
		}
		else
		{
			secondExhaustPos = firstExhaustPos;
			secondExhaustPos.x *= -1.0f;
		}

		if(physical->bTouchingWater)
		{
			CVector point = TransformPoint(&leanMat, secondExhaustPos);

			if(GetWaterLevel(point.x, point.y, point.z, &level, true, nullptr))
			{
				if(level >= point.z)
				{
					secondExhaustUnderWater = true;
				}
			}
		}
	}

	RwFrame* frame = reinterpret_cast<RwFrame*>(automobile->m_pRwObject->parent);
	RwMatrix* rwMatrix = reinterpret_cast<RwMatrix*>((uintptr_t)frame + 0x10);

	FxSystem_c* firstExhaustFxSystem = reinterpret_cast<FxSystem_c*>(automobile->pNitroParticle[0]);
	if(firstExhaustFxSystem)
	{
		firstExhaustFxSystem->SetConstTime(1, std::fabs(power));
		if(firstExhaustFxSystem->m_nPlayStatus == eFxSystemPlayStatus::FX_PLAYING && firstExhaustUnderWater)
		{
			firstExhaustFxSystem->Stop();
		}
		else if(firstExhaustFxSystem->m_nPlayStatus == eFxSystemPlayStatus::FX_STOPPED && !firstExhaustUnderWater)
		{
			firstExhaustFxSystem->Play();
		}
	}
	else if(!firstExhaustUnderWater && rwMatrix)
	{
		RwV3d pos = firstExhaustPos.ToRwV3d();
		firstExhaustFxSystem = g_fxMan.CreateFxSystem((char*)"nitro", &pos, rwMatrix, true);
		automobile->pNitroParticle[0] = firstExhaustFxSystem;
		if(firstExhaustFxSystem)
		{
			firstExhaustFxSystem->SetLocalParticles(true);
			firstExhaustFxSystem->Play();
		}
	}

	if(doubleExhaust)
	{
		FxSystem_c* secondExhaustFxSystem = reinterpret_cast<FxSystem_c*>(automobile->pNitroParticle[1]);
		if(secondExhaustFxSystem)
		{
			secondExhaustFxSystem->SetConstTime(1, std::fabs(power));
			if(secondExhaustFxSystem->m_nPlayStatus == eFxSystemPlayStatus::FX_PLAYING && secondExhaustUnderWater)
			{
				secondExhaustFxSystem->Stop();
			}
			else if(secondExhaustFxSystem->m_nPlayStatus == eFxSystemPlayStatus::FX_STOPPED && !secondExhaustUnderWater)
			{
				secondExhaustFxSystem->Play();
			}
		}
		else if(!firstExhaustUnderWater && rwMatrix)
		{
			RwV3d pos = secondExhaustPos.ToRwV3d();
			secondExhaustFxSystem = g_fxMan.CreateFxSystem((char*)"nitro", &pos, rwMatrix, true);
			automobile->pNitroParticle[1] = secondExhaustFxSystem;
			if(secondExhaustFxSystem)
			{
				secondExhaustFxSystem->SetLocalParticles(1);
				secondExhaustFxSystem->Play();
			}
		}
	}
}

CBike* FindBikeFromMatrix(RwMatrix* mat)
{
	for(int i = 0; i < CPools::ms_pVehiclePool->m_nSize; ++i)
	{
		CAutomobile* automobile = reinterpret_cast<CAutomobile*>(CPools::ms_pVehiclePool->GetAt(i));

		if(!automobile) continue;

		if(automobile->m_nVehicleSubClass == 9)
		{
			uintptr_t address0 = reinterpret_cast<uintptr_t>(mat);
			uintptr_t address1 = reinterpret_cast<uintptr_t>(automobile->m_pRwObject->parent) + 0x10;

			if(address0 == address1)
			{
				return reinterpret_cast<CBike*>(automobile);
			}
		}
	}

	return nullptr;
}

void __fastcall GetCompositeMatrixHook(FxSystem_c* _this, void* edx, RwMatrixTag* out)
{
	if(_this->m_pParentMatrix)
	{
		if(CBike* bike = FindBikeFromMatrix(_this->m_pParentMatrix))
		{
			RwMatrix rwLeanMat;
			bike->m_mLeanMatrix.CopyToRwMatrix(&rwLeanMat);
			RwMatrixMultiply(out, &_this->m_localMatrix, &rwLeanMat);
		}
		else
		{
			RwMatrixMultiply(out, &_this->m_localMatrix, _this->m_pParentMatrix);
		}
	}
	else
	{
		*out = _this->m_localMatrix;
	}
}

void __stdcall RemoveNitroUpgrades(CBike* bike)
{
	for(int i = 0; i < 15; ++i)
	{
		if(bike->m_anUpgrades[i] == 1009 || bike->m_anUpgrades[i] == 1008 || bike->m_anUpgrades[i] == 1010)
		{
			bike->m_anUpgrades[i] = -1;
		}
	}
}

void __declspec(naked) AllowBikesToAddNitro_thunk() //Original function by LINK/2012
{
	_asm
	{
		mov eax, dword ptr[esi + 0x590]
		test eax, eax	//CAutomobile
		jz _AllowNitro
		mov eax, dword ptr[esi + 0x594]
		cmp eax, 9		//CBike
		jz _AllowNitro_Bike

		//Don't allow nitro
		mov eax, 0x6D3200
		jmp eax

		_AllowNitro_Bike:
			pushad
			push esi
			call RemoveNitroUpgrades
			popad

		_AllowNitro:
			mov eax, 0x6D3198
			jmp eax
	}
}

void __declspec(naked) AllowBikesToRemoveNitro_thunk() //Original function by LINK/2012
{
	_asm
	{
		mov eax, dword ptr[esi + 0x590]
		test eax, eax	//CAutomobile
		jz _AllowNitro
		mov eax, dword ptr[esi + 0x594]
		cmp eax, 9		//CBike
		jz _AllowNitro_Bike

		//Don't allow nitro
		mov eax, 0x6D32F3
		jmp eax

		_AllowNitro_Bike:
			pushad
			push esi
			call RemoveNitroUpgrades
			popad

		_AllowNitro:
			mov eax, 0x6D32D2
			jmp eax
	}
}

void LoadIniValues()
{
	CIniReader ini("AdvancedNitroSystem.ini");
	disableOnSAMP = ini.ReadInteger("configs", "DisableOnSAMP", 1);
	useBikeNitro = ini.ReadInteger("ProperBikeNitro", "UseBikeNitro", 0);
}

void AddCBikeProcessControlHooks()
{	
	GetColModel.before += [](CVehicle* x)
	{
		GetColModelHook(x);
	};
	SkipPhysics.before += [](CPhysical* x)
	{
		SkipPhysicsHook(x);
	};
	patch::RedirectCall(0x6BA63D, CalculateDriveAccelerationHook); //Apply nitro acceleration on bikes
}

void FixDesync(CVehicle* veh) //Fix problem with vehik
{
	if(veh->m_nVehicleSubClass == 9)
	{
		veh->UpdateRwFrame();
	}
}

struct Main
{
    Main()
	{
		LoadIniValues();

		if(GetModuleHandleA("SAMP.dll") && disableOnSAMP) return;

		CBikeDestructor.before += [](CBike* x)
		{
			CBikeDestructorHook(x); //Original destructor doesn't destroy nitro fx
		};

		Events::vehicleRenderEvent.before += [](CVehicle* x)
		{
			FixDesync(x);
		};

		AddCBikeProcessControlHooks(); //Implements code from CAutomobile::ProcessControl inside CBike::ProcessControl

		ProcessVehicleRoadNoise.before += [](CAEVehicleAudioEntity* x, cVehicleParams* a2)
		{
			ProcessVehicleRoadNoiseHook(x, a2); //Makes bikes process nitro sound
		};

		SetupSuspensionLines.before += [](CBike* x)
		{
			SetupSuspensionLinesHook(x);
		};

		patch::RedirectCall(0x4A2DC7, GetCompositeMatrixHook); //Fixes nitro fx position on bikes

		patch::RedirectCall(0x6A40E1, DoNitroEffectHook);
		patch::RedirectCall(0x6A406B, DoNitroEffectHook);
		patch::RedirectCall(0x6A405A, DoNitroEffectHook);

		patch::Nop(0x704DFB, 8); //Allows bikes to use nitro blur effect

		patch::Nop(0x6D318E, 5);
		injector::MakeJMP(0x6D318E, &AllowBikesToAddNitro_thunk);

		patch::Nop(0x6D32C8, 5);
		injector::MakeJMP(0x6D32C8, &AllowBikesToRemoveNitro_thunk);
	}
} gInstance;
