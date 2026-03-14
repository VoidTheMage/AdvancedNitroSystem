SCRIPT_START
{
NOP

SCRIPT_NAME ANSYSTE

LVAR_INT child lastVeh scplayer veh mod texture iTemp0 iTemp1 fontR fontG fontB iconR iconG iconB settings nitrousControlHook alignment ansASI log

LVAR_FLOAT fTemp0 fTemp1 fTemp2 fTemp3 offsetX offsetY offsetXNR offsetYNR iconOffsetX iconOffsetY currentNos

GET_LABEL_POINTER Settings settings
GET_PLAYER_CHAR 0 scplayer

OPEN_FILE "cleo/AdvancedNitroSystem.log" 119 log
WRITE_FORMATTED_STRING_TO_FILE log "Advanced Nitro System V3.2 by Void the Mage%c" 10

IF IS_ON_SAMP
    WRITE_FORMATTED_STRING_TO_FILE log "SAMP detected%c" 10
    READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "DisableOnSAMP" iTemp0
    IF iTemp0 = TRUE
        WRITE_FORMATTED_STRING_TO_FILE log "Terminating this script%c" 10
        TERMINATE_THIS_CUSTOM_SCRIPT
    ENDIF
ENDIF

GOSUB ReadMainSettings
GOSUB ReadHUDSettings

IF LOAD_DYNAMIC_LIBRARY "AdvancedNitroSystemSA.asi" ansASI
    WRITE_FORMATTED_STRING_TO_FILE log "Success to load AdvancedNitroSystemSA.asi%c" 10
ELSE
    PRINT_STRING_NOW "Failed to load AdvancedNitroSystemSA.asi" 5000
    WRITE_FORMATTED_STRING_TO_FILE log "Failed to load AdvancedNitroSystemSA.asi%c" 10
    WRITE_FORMATTED_STRING_TO_FILE log "Terminating this script%c" 10
    TERMINATE_THIS_CUSTOM_SCRIPT
ENDIF

IF LOAD_DYNAMIC_LIBRARY "ProperBikeNitroSA.asi" iTemp0
    WRITE_FORMATTED_STRING_TO_FILE log "Success to load ProperBikeNitroSA.asi%c" 10
ELSE
    WRITE_FORMATTED_STRING_TO_FILE log "Failed to load ProperBikeNitroSA.asi%c" 10
ENDIF

READ_STRUCT_OFFSET settings 36 4 iTemp0 //ConsumableMode
IF iTemp0 = TRUE
    GET_VAR_POINTER nitrousControlHook iTemp1
    GET_DYNAMIC_LIBRARY_PROCEDURE "Subscribe" ansASI iTemp0
    CALL_FUNCTION iTemp0 1 1 iTemp1
ENDIF

main_loop:
GET_DYNAMIC_LIBRARY_PROCEDURE "SetNitroValue" ansASI iTemp0
CALL_FUNCTION iTemp0 1 1 0

IF TEST_CHEAT RELOADNOSHUD
    GOSUB ReadHUDSettings

    GET_DYNAMIC_LIBRARY_PROCEDURE "ReloadNos" ansASI iTemp0
    CALL_FUNCTION iTemp0 0 0
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

                    GET_DYNAMIC_LIBRARY_PROCEDURE "SetNitroValue" ansASI iTemp0
                    CALL_FUNCTION iTemp0 1 1 currentNos

                    READ_STRUCT_OFFSET settings 64 4 iTemp0

                    IF iTemp0 = TRUE //drawNosHud
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
                    GET_THIS_SCRIPT_STRUCT iTemp0
                    STREAM_CUSTOM_SCRIPT NITROINSTANCE.CS iTemp0 veh settings
                ENDIF
            ENDIF
        ENDIF
    ENDIF
ENDIF

WAIT 0
GOTO main_loop

DrawUI:
GET_DYNAMIC_LIBRARY_PROCEDURE "DrawBar" ansASI iTemp0
CALL_FUNCTION iTemp0 1 1 currentNos

READ_STRUCT_OFFSET settings 68 4 iTemp0

IF iTemp0 = TRUE //drawNosIcon
    GET_DYNAMIC_LIBRARY_PROCEDURE "IsRadarVisibleNoBlink" ansASI iTemp0
    CALL_FUNCTION_RETURN iTemp0 0 0 iTemp0

    IF iTemp0 = TRUE
        fTemp0 = offsetX
        fTemp1 = offsetY
    ELSE
        fTemp0 = offsetXNR
        fTemp1 = offsetYNR
    ENDIF

    GET_LABEL_POINTER Buffer iTemp1
    
    READ_STRUCT_OFFSET settings 36 4 iTemp0 //ConsumableMode
    IF iTemp0 = FALSE
        SWITCH mod
            CASE 1009
                STRING_FORMAT iTemp1 "2"
            BREAK
            CASE 1008
                STRING_FORMAT iTemp1 "5"
            BREAK
            CASE 1010
                STRING_FORMAT iTemp1 "10"
            BREAK
            DEFAULT
                STRING_FORMAT iTemp1 "10"
            BREAK
        ENDSWITCH
    ELSE
        GET_VEHICLE_POINTER veh iTemp0
        READ_STRUCT_OFFSET iTemp0 0x48A 1 iTemp0 //Nos amount

        STRING_FORMAT iTemp1 "%d" iTemp0
    ENDIF

    GET_FIXED_XY_ASPECT_RATIO fTemp0 fTemp1 fTemp0 fTemp1
    DRAW_STRING_EXT $iTemp1 DRAW_EVENT_AFTER_HUD fTemp0 fTemp1 0.47 0.94 TRUE FONT_PRICEDOWN 0 alignment 0.0 2 fontR fontG fontB 255 2 0 0 0 0 255 0 0 0 0 0

    GET_DYNAMIC_LIBRARY_PROCEDURE "IsRadarVisibleNoBlink" ansASI iTemp0
    CALL_FUNCTION_RETURN iTemp0 0 0 iTemp0

    IF iTemp0 = TRUE
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

READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "DrawHUD" iTemp0
WRITE_STRUCT_OFFSET settings 64 4 iTemp0

READ_INT_FROM_INI_FILE "cleo/AdvancedNitroSystem.ini" "configs" "DrawNosIcon" iTemp0
WRITE_STRUCT_OFFSET settings 68 4 iTemp0

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
00 00 00 00 //drawHud
00 00 00 00 //drawNosIcon
ENDDUMP

Buffer:
DUMP
00 00 00 00
00 00 00 00
00 00 00 00
00 00 00 00
00 00 00 00
00 00 00 00
00 00 00 00
00 00 00 00 //32 bytes
ENDDUMP