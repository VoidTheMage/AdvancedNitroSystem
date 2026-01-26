SCRIPT_START
{
NOP

SCRIPT_NAME ANSYSTE

LVAR_INT child lastVeh scplayer this veh mod texture drawHUD drawNosIcon iTemp0 iTemp1 fontR fontG fontB iconR iconG iconB settings nitrousControlHook alignment

LVAR_FLOAT fTemp0 fTemp1 offsetX offsetY offsetXNR offsetYNR iconOffsetX iconOffsetY currentNos

LVAR_TEXT_LABEL string

GET_LABEL_POINTER Settings settings
GET_THIS_SCRIPT_STRUCT this
GET_PLAYER_CHAR 0 scplayer

READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "DisableOnSAMP" iTemp0
IF IS_ON_SAMP
AND iTemp0 = TRUE
    TERMINATE_THIS_CUSTOM_SCRIPT
ENDIF

GOSUB ReadMainSettings
GOSUB ReadHUDSettings

READ_STRUCT_OFFSET settings 36 4 iTemp0 //ConsumableMode
IF iTemp0 = TRUE
    GET_VAR_POINTER nitrousControlHook iTemp1
    IF LOAD_DYNAMIC_LIBRARY "AdvancedNitroSystemSA.asi" iTemp0
        GET_DYNAMIC_LIBRARY_PROCEDURE "Subscribe" iTemp0 iTemp0
        CALL_FUNCTION iTemp0 1 1 iTemp1
    ENDIF
ENDIF

main_loop:
IF LOAD_DYNAMIC_LIBRARY "AdvancedNitroSystemSA.asi" iTemp0
    GET_DYNAMIC_LIBRARY_PROCEDURE "SetNitroValue" iTemp0 iTemp1
    CALL_FUNCTION iTemp1 1 1 0
ENDIF

IF TEST_CHEAT RELOADNOSHUD
    GOSUB ReadHUDSettings
    IF LOAD_DYNAMIC_LIBRARY "AdvancedNitroSystemSA.asi" iTemp0
        GET_DYNAMIC_LIBRARY_PROCEDURE "ReloadNos" iTemp0 iTemp1
        CALL_FUNCTION iTemp1 0 0
    ENDIF
    PRINT_STRING_NOW "Advanced Nitro System HUD Reloaded" 2000
ENDIF

IF IS_CHAR_SITTING_IN_ANY_CAR scplayer
    GET_CAR_CHAR_IS_USING scplayer veh

    IF GOSUB CheckIfVehIsValid
        GET_DRIVER_OF_CAR veh iTemp0
        GET_CURRENT_CAR_MOD veh 8 mod //This command was modified in the asi file
        IF scplayer = iTemp0 //Is player the driver
            IF veh = lastVeh
                IF NOT mod = -1
                    IF nitrousControlHook = TRUE
                        SET_SCRIPT_VAR child 3 1.0
                        nitrousControlHook = FALSE
                    ENDIF

                    GET_SCRIPT_VAR child 3 currentNos
                    CLAMP_FLOAT currentNos 0.0 1.0 currentNos

                    IF LOAD_DYNAMIC_LIBRARY "AdvancedNitroSystemSA.asi" iTemp0
                        GET_DYNAMIC_LIBRARY_PROCEDURE "SetNitroValue" iTemp0 iTemp1
                        CALL_FUNCTION iTemp1 1 1 currentNos
                    ENDIF

                    IF drawHUD = TRUE
                    AND IS_HUD_VISIBLE
                    AND IS_PLAYER_CONTROL_ON 0
                    AND IS_PLAYER_PLAYING 0
                        GOSUB DrawUI
                    ENDIF
                ELSE
                    lastVeh = 0
                ENDIF
            ELSE
                IF NOT mod = -1
                    lastVeh = veh
                    STREAM_CUSTOM_SCRIPT NITROINSTANCE.CS this veh settings
                ENDIF
            ENDIF
        ENDIF
    ENDIF
ENDIF

WAIT 0
GOTO main_loop

DrawUI:
IF LOAD_DYNAMIC_LIBRARY "AdvancedNitroSystemSA.asi" iTemp0
    GET_DYNAMIC_LIBRARY_PROCEDURE "DrawBar" iTemp0 iTemp1
    CALL_FUNCTION iTemp1 1 1 currentNos
ENDIF

IF drawNosIcon = TRUE
    IF IS_RADAR_VISIBLE
        fTemp0 = offsetX
        fTemp1 = offsetY
    ELSE
        fTemp0 = offsetXNR
        fTemp1 = offsetYNR
    ENDIF
    
    READ_STRUCT_OFFSET settings 36 4 iTemp0 //ConsumableMode
    IF iTemp0 = FALSE
        SWITCH mod
            CASE 1009
                STRING_FORMAT string "2"
            BREAK
            CASE 1008
                STRING_FORMAT string "5"
            BREAK
            CASE 1010
                STRING_FORMAT string "10"
            BREAK
            DEFAULT
                STRING_FORMAT string "10"
            BREAK
        ENDSWITCH
    ELSE
        GET_VEHICLE_POINTER veh iTemp0
        READ_STRUCT_OFFSET iTemp0 0x48A 1 iTemp0 //Nos amount

        STRING_FORMAT string "%d" iTemp0
    ENDIF

    GET_FIXED_XY_ASPECT_RATIO fTemp0 fTemp1 fTemp0 fTemp1
    DRAW_STRING_EXT $string DRAW_EVENT_AFTER_HUD fTemp0 fTemp1 0.47 0.94 TRUE FONT_PRICEDOWN 0 alignment 0.0 2 fontR fontG fontB 255 2 0 0 0 0 255 0 0 0 0 0

    IF IS_RADAR_VISIBLE
        fTemp0 = offsetX
        fTemp1 = offsetY
    ELSE
        fTemp0 = offsetXNR
        fTemp1 = offsetYNR
    ENDIF

    GOSUB FineTuneIconPos

    GET_FIXED_XY_ASPECT_RATIO fTemp0 fTemp1 fTemp0 fTemp1
    GET_TEXTURE_FROM_SPRITE 2 texture
    DRAW_TEXTURE_PLUS texture DRAW_EVENT_AFTER_HUD fTemp0 fTemp1 17.0 17.0 0.0 0.0 TRUE 0 0 iconR iconG iconB 255 //Drawn separately to avoid weird bleeding
    GET_TEXTURE_FROM_SPRITE 1 texture
    DRAW_TEXTURE_PLUS texture DRAW_EVENT_AFTER_HUD fTemp0 fTemp1 17.0 17.0 0.0 0.0 TRUE 0 0 0 0 0 255 //Drawn separately to avoid weird bleeding
ENDIF

RETURN

FineTuneIconPos:
READ_STRUCT_OFFSET settings 36 4 iTemp0 //ConsumableMode
IF iTemp0 = FALSE
    IF mod = 1010
        iTemp0 = TRUE
    ELSE
        iTemp0 = FALSE
    ENDIF
ELSE
    GET_VEHICLE_POINTER veh iTemp0
    READ_STRUCT_OFFSET iTemp0 0x48A 1 iTemp0 //Nos amount

    IF iTemp0 = 10
        iTemp0 = TRUE
    ELSE
        iTemp0 = FALSE
    ENDIF
ENDIF

IF iTemp0 = TRUE //Optimization trick
    IF alignment = 0
        fTemp0 -= 16.4
    ENDIF
    IF alignment = 1
        fTemp0 -= 5.9
    ENDIF
    IF alignment = 2
        fTemp0 -= 26.7
    ENDIF
ELSE
    IF alignment = 0
        fTemp0 -= 11.0
    ENDIF
    IF alignment = 1
        fTemp0 -= 5.9
    ENDIF
    IF alignment = 2
        fTemp0 -= 16.4
    ENDIF
ENDIF

fTemp1 += 8.55

RETURN

CheckIfVehIsValid:
GET_VEHICLE_SUBCLASS veh iTemp0

IF iTemp0 = 0
OR iTemp0 = 1
OR iTemp0 = 2
OR iTemp0 = 9
    RETURN_TRUE
ELSE
    RETURN_FALSE
ENDIF

RETURN

ReadMainSettings:
READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "Nos2XDuration" fTemp0
fTemp1 = 0.02
fTemp1 /= fTemp0
WRITE_STRUCT_OFFSET settings 0 4 fTemp1

READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "Nos5XDuration" fTemp0
fTemp1 = 0.02
fTemp1 /= fTemp0
WRITE_STRUCT_OFFSET settings 4 4 fTemp1

READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "Nos10XDuration" fTemp0
fTemp1 = 0.02
fTemp1 /= fTemp0
WRITE_STRUCT_OFFSET settings 8 4 fTemp1

READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "RefillRate" fTemp0
WRITE_STRUCT_OFFSET settings 12 4 fTemp0

READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "RefillDelay" iTemp0
WRITE_STRUCT_OFFSET settings 16 4 iTemp0

READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "InfiniteNos" iTemp0
WRITE_STRUCT_OFFSET settings 20 4 iTemp0

READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "UseNitroPurge" iTemp0
WRITE_STRUCT_OFFSET settings 24 4 iTemp0

READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "NitroPurgeVolume" fTemp0
WRITE_STRUCT_OFFSET settings 28 4 fTemp0

READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "NosKey" iTemp0
WRITE_STRUCT_OFFSET settings 32 4 iTemp0

READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "ConsumableMode" iTemp0
WRITE_STRUCT_OFFSET settings 36 4 iTemp0

READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "MinimumUsableNos" fTemp0
WRITE_STRUCT_OFFSET settings 40 4 fTemp0

READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "PurgeConsumption" fTemp0
WRITE_STRUCT_OFFSET settings 44 4 fTemp0

READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "ConsumableDuration" fTemp0
fTemp1 = 20.0 / fTemp0
fTemp0 = 0.001 * fTemp1
WRITE_STRUCT_OFFSET settings 48 4 fTemp0

READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "DisableIfLookingSideKBM" iTemp0
WRITE_STRUCT_OFFSET settings 52 4 iTemp0

READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "ToggleNosKey" iTemp0
WRITE_STRUCT_OFFSET settings 56 4 iTemp0

READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "PurgeKey" iTemp0
WRITE_STRUCT_OFFSET settings 60 4 iTemp0

RETURN

ReadHUDSettings:
READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "IconR" iconR
READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "IconG" iconG
READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "IconB" iconB
READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "FontR" fontR
READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "FontG" fontG
READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "FontB" fontB
READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "OffsetX" offsetX
READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "OffsetY" offsetY
READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "OffsetXNoRadar" offsetXNR
READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "OffsetYNoRadar" offsetYNR
READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "IconOffsetX" iconOffsetX
READ_FLOAT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "IconOffsetY" iconOffsetY
READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "IconAlignment" alignment

offsetX += iconOffsetX
offsetX /= 1920.0
offsetX *= 640.0
offsetY += iconOffsetY
offsetY /= 1080.0
offsetY *= 448.0

offsetXNR += iconOffsetX
offsetXNR /= 1920.0
offsetXNR *= 640.0
offsetYNR += iconOffsetY
offsetYNR /= 1080.0
offsetYNR *= 448.0

READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini", "configs", "DrawHUD") drawHUD
READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini", "configs", "DrawNosIcon") drawNosIcon
LOAD_TEXTURE_DICTIONARY ansyste
LOAD_SPRITE 1 NOSICO
LOAD_SPRITE 2 NOSICOBG

RETURN

SCRIPT_END
}

Settings:
DUMP
00 00 00 00 //nos2xDuration
00 00 00 00 //nos5xDuration
00 00 00 00 //nos10xDuration
00 00 00 00 //refillRate
00 00 00 00 //refillDelay
00 00 00 00 //infiniteNos
00 00 00 00 //usePurge
00 00 00 00 //purgeVolume
00 00 00 00 //nosKey
00 00 00 00 //consumableMode
00 00 00 00 //minimumUsableNos
00 00 00 00 //purgeConsumption
00 00 00 00 //consumableDuration
00 00 00 00 //disableIfLookingSidewaysKBM
00 00 00 00 //toggleNosKey
00 00 00 00 //purgeKey
ENDDUMP