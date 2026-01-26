SCRIPT_START
{
NOP

SCRIPT_NAME ANSINST

LVAR_INT  parent veh settings

LVAR_FLOAT currentNos

LVAR_INT mod currentNosPointer scplayer this vehPointer wasUsingNos fx0 fx1 usingPurge tryingToUsePurge audioStream iTemp0 iTemp1 iTemp2 nosToggle wasToggleReleased wasNosEmpty isBike

LVAR_FLOAT fTemp0 fTemp1 fTemp2

IF parent = FALSE //Delete if wasn't instantiated by parent
    TERMINATE_THIS_CUSTOM_SCRIPT
ENDIF

IF NOT DOES_VEHICLE_EXIST veh //Safety measure
    TERMINATE_THIS_CUSTOM_SCRIPT
ENDIF

currentNos = 1.0
wasToggleReleased = TRUE

GET_VAR_POINTER currentNos currentNosPointer
GET_THIS_SCRIPT_STRUCT this
SET_SCRIPT_VAR parent 0 this
GET_PLAYER_CHAR 0 scplayer
GET_VEHICLE_POINTER veh vehPointer

GET_VEHICLE_SUBCLASS veh iTemp0
IF iTemp0 = 9
    isBike = TRUE
ENDIF

READ_STRUCT_OFFSET settings 24 4 iTemp0 //UseNitroPurge
IF iTemp0 = TRUE
AND isBike = FALSE
    GET_VAR_POINTER fTemp0 iTemp1
    GET_CAR_MODEL veh iTemp0
    GET_MODEL_INFO iTemp0 iTemp0
    CALL_METHOD 0x4C7D20 iTemp0 3 0 0 iTemp1 0

    fTemp1 -= 0.1
    fTemp2 -= 0.1
    
    CREATE_FX_SYSTEM_ON_CAR_WITH_DIRECTION "EXTINGUISHER" veh fTemp0 fTemp1 fTemp2 -1.0 0.0 1.0 TRUE fx0
    fTemp0 *= -1.0
    CREATE_FX_SYSTEM_ON_CAR_WITH_DIRECTION "EXTINGUISHER" veh fTemp0 fTemp1 fTemp2 1.0 0.0 1.0 TRUE fx1

    LOAD_3D_AUDIO_STREAM "cleo/audio/Nitrous Purge (Junior_Djjr).mp3" audioStream

    SET_AUDIO_STREAM_LOOPED audioStream TRUE
    SET_PLAY_3D_AUDIO_STREAM_AT_CAR audioStream veh
ENDIF

READ_STRUCT_OFFSET settings 36 4 iTemp0 //ConsumableMode
IF iTemp0 = TRUE
    READ_STRUCT_OFFSET vehPointer 0x48A 1 iTemp0 //Nos amount
    IF iTemp0 = 0
        currentNos = 0.0 //Dont fill up currentNos if Nos amount reached 0
        wasNosEmpty = TRUE
    ENDIF
ENDIF

main_loop:
READ_STRUCT_OFFSET settings 36 4 iTemp0 //ConsumableMode
IF iTemp0 = TRUE
    IF currentNos <= 0.0
        READ_STRUCT_OFFSET vehPointer 0x48A 1 iTemp0 //Nos amount
        IF iTemp0 > 1
            fTemp0 = 1.0 - currentNos
            currentNos = fTemp0

            IF wasNosEmpty = FALSE //Avoid subtracting after fill up from hook
                iTemp0 --
                CLAMP_INT iTemp0 0 10 iTemp0
                WRITE_STRUCT_OFFSET vehPointer 0x48A 1 iTemp0 //Nos amount
            ENDIF
        ELSE
            WRITE_STRUCT_OFFSET vehPointer 0x48A 1 0 //Nos amount
            wasNosEmpty = TRUE
        ENDIF
    ELSE
        wasNosEmpty = FALSE
    ENDIF
ENDIF

CLAMP_FLOAT currentNos 0.0 1.0 currentNos
WAIT 0

GOSUB CheckIfVehExists

GOSUB CheckIfNosExists

READ_STRUCT_OFFSET settings 36 4 iTemp0 //ConsumableMode
IF iTemp0 = FALSE
    GOSUB SetNosAmount
ENDIF

IF GOSUB IsPlayerDriving
    SET_SCRIPT_VAR parent 0 this
    SET_SCRIPT_VAR parent 1 veh

    IF NOT IS_PLAYER_CONTROL_ON 0
    OR NOT IS_PLAYER_PLAYING 0
    OR IS_CAR_IN_WATER veh
    OR NOT IS_CAR_ENGINE_ON veh
        IF wasUsingNos = TRUE
            WRITE_STRUCT_OFFSET vehPointer 0x8A4 4 1.0
            GOSUB StopNitroPurge
            wasUsingNos = FALSE
        ENDIF

        nosToggle = FALSE
        wasToggleReleased = TRUE

        TIMERA = 0
        GOTO main_loop
    ENDIF

    IF currentNos > 0.0
        READ_STRUCT_OFFSET settings 36 4 iTemp0 //ConsumableMode
        IF iTemp0 = TRUE
            fTemp0 = 0.0
        ELSE
            READ_STRUCT_OFFSET settings 40 4 fTemp0 //MinimumUsableNOS
        ENDIF

        IF wasUsingNos = TRUE //Without this wouldn't be possible to use the whole nitro
        OR currentNos > fTemp0
            IF GOSUB IsPressingNosButton
                IF GOSUB IsAccelerating
                    READ_STRUCT_OFFSET settings 24 4 iTemp0 //UseNitroPurge
                    IF iTemp0 = TRUE
                    AND isBike = FALSE
                        READ_STRUCT_OFFSET settings 32 4 iTemp0
                        READ_STRUCT_OFFSET settings 60 4 iTemp1
                        IF iTemp0 = iTemp1
                        OR IS_PC_USING_JOYPAD
                            GOSUB UseNos
                        ELSE
                            IF GOSUB IsPressingPurgeButton
                                GOSUB UsePurge
                            ELSE
                                GOSUB UseNos
                            ENDIF
                        ENDIF
                    ELSE
                        GOSUB UseNos
                    ENDIF
                ELSE
                    READ_STRUCT_OFFSET settings 24 4 iTemp0 //UseNitroPurge
                    IF iTemp0 = TRUE
                    AND isBike = FALSE
                        IF GOSUB IsPressingPurgeButton
                            READ_STRUCT_OFFSET settings 32 4 iTemp0
                            READ_STRUCT_OFFSET settings 60 4 iTemp1
                            IF iTemp0 = iTemp1
                            OR IS_PC_USING_JOYPAD
                                IF TIMERB > 500
                                    GOSUB UsePurge
                                ELSE
                                    wasToggleReleased = TRUE
                                    GOSUB StopNosUsage
                                ENDIF
                            ELSE
                                GOSUB UsePurge
                            ENDIF
                        ELSE
                            GOSUB UseNos
                        ENDIF
                    ELSE
                        GOSUB UseNos
                    ENDIF
                ENDIF
            ELSE
                READ_STRUCT_OFFSET settings 24 4 iTemp0 //UseNitroPurge
                IF iTemp0 = TRUE
                AND isBike = FALSE
                    READ_STRUCT_OFFSET settings 32 4 iTemp0
                    READ_STRUCT_OFFSET settings 60 4 iTemp1
                    IF iTemp0 = iTemp1
                    OR IS_PC_USING_JOYPAD
                        IF GOSUB IsAccelerating
                            GOSUB StopNosUsage
                        ELSE
                            IF GOSUB IsPressingPurgeButton
                                IF TIMERB > 500
                                    GOSUB UsePurge
                                ELSE
                                    wasToggleReleased = TRUE
                                    tryingToUsePurge = TRUE
                                    GOSUB StopNosUsage
                                ENDIF
                            ELSE
                                GOSUB StopNosUsage
                            ENDIF
                        ENDIF
                    ELSE
                        IF GOSUB IsPressingPurgeButton
                            GOSUB UsePurge
                        ELSE
                            GOSUB StopNosUsage
                        ENDIF
                    ENDIF
                ELSE
                    GOSUB StopNosUsage
                ENDIF
            ENDIF
        ELSE
            GOSUB StopNosUsage
        ENDIF
    ELSE
        GOSUB StopNosUsage
    ENDIF
ELSE
    GOSUB StopNosUsage
ENDIF

GOTO main_loop

IsPressingNosButton:
READ_STRUCT_OFFSET 0xB6F1A8 0 2 iTemp0 //Cam mode
IF iTemp0 = 55 //AIMWEAPON_FROMCAR compatibility with driveby mod
    RETURN_FALSE
ELSE
    IF IS_PC_USING_JOYPAD
        READ_STRUCT_OFFSET settings 56 4 iTemp0

        IF iTemp0 = TRUE //Toggle
            GET_CONTROLLER_MODE iTemp0 //Ginput control scheme
            IF iTemp0 = 0
                IF IS_BUTTON_JUST_PRESSED 0 CIRCLE
                //OR IS_BUTTON_JUST_PRESSED 0 LEFTSHOULDER1
                    IF nosToggle = TRUE
                        nosToggle = FALSE
                    ELSE
                        READ_STRUCT_OFFSET settings 24 4 iTemp0
                        IF iTemp0 = TRUE //Use purge
                        AND isBike = FALSE
                            IF GOSUB IsAccelerating
                                nosToggle = TRUE
                                wasToggleReleased = FALSE
                            ENDIF
                        ELSE
                            nosToggle = TRUE
                        ENDIF
                    ENDIF
                ELSE
                    IF IS_BUTTON_PRESSED 0 CIRCLE
                    //OR IS_BUTTON_PRESSED 0 LEFTSHOULDER1
                        IF GOSUB IsAccelerating
                            IF usingPurge = TRUE
                            OR tryingToUsePurge = TRUE
                                nosToggle = TRUE
                                wasToggleReleased = FALSE
                                tryingToUsePurge = FALSE
                            ENDIF
                        ENDIF
                    ELSE
                        wasToggleReleased = TRUE
                    ENDIF
                ENDIF
            ELSE
                IF IS_BUTTON_JUST_PRESSED 0 SQUARE
                    IF nosToggle = TRUE
                        nosToggle = FALSE
                    ELSE
                        READ_STRUCT_OFFSET settings 24 4 iTemp0
                        IF iTemp0 = TRUE //Use purge
                        AND isBike = FALSE
                            IF GOSUB IsAccelerating
                                nosToggle = TRUE
                                wasToggleReleased = FALSE
                            ENDIF
                        ELSE
                            nosToggle = TRUE
                        ENDIF
                    ENDIF
                ELSE
                    IF IS_BUTTON_PRESSED 0 SQUARE
                        IF GOSUB IsAccelerating
                            IF usingPurge = TRUE
                            OR tryingToUsePurge = TRUE
                                nosToggle = TRUE
                                wasToggleReleased = FALSE
                                tryingToUsePurge = FALSE
                            ENDIF
                        ENDIF
                    ELSE
                        wasToggleReleased = TRUE
                    ENDIF
                ENDIF
            ENDIF

            IF nosToggle = TRUE
                RETURN_TRUE
            ELSE
                RETURN_FALSE
            ENDIF
        ELSE
            GET_CONTROLLER_MODE iTemp0 //Ginput control scheme
            IF iTemp0 = 0
                IF IS_BUTTON_PRESSED 0 CIRCLE
                //OR IS_BUTTON_PRESSED 0 LEFTSHOULDER1
                    RETURN_TRUE
                ELSE
                    RETURN_FALSE
                ENDIF
            ELSE
                IF IS_BUTTON_PRESSED 0 SQUARE
                    RETURN_TRUE
                ELSE
                    RETURN_FALSE
                ENDIF
            ENDIF
        ENDIF
    ELSE        
        READ_STRUCT_OFFSET settings 56 4 iTemp0
        IF iTemp0 = TRUE //Toggle
            READ_STRUCT_OFFSET settings 52 4 iTemp0
            IF iTemp0 = TRUE //Looking sideways
                IF GOSUB IsLookingSideways
                    IF nosToggle = TRUE
                        RETURN_TRUE
                    ELSE
                        RETURN_FALSE
                    ENDIF
                    RETURN
                ENDIF
            ENDIF
            
            READ_STRUCT_OFFSET settings 32 4 iTemp0
            IF IS_KEY_JUST_PRESSED iTemp0
                IF nosToggle = TRUE
                    nosToggle = FALSE
                ELSE
                    READ_STRUCT_OFFSET settings 60 4 iTemp1
                    READ_STRUCT_OFFSET settings 24 4 iTemp2
                    IF iTemp0 = iTemp1
                    AND iTemp2 = TRUE //Use purge
                    AND isBike = FALSE
                        IF GOSUB IsAccelerating
                            nosToggle = TRUE
                            wasToggleReleased = FALSE
                        ENDIF
                    ELSE
                        nosToggle = TRUE
                    ENDIF
                ENDIF
            ELSE
                IF IS_KEY_PRESSED iTemp0
                    IF GOSUB IsAccelerating
                        IF usingPurge = TRUE
                        OR tryingToUsePurge = TRUE
                            nosToggle = TRUE
                            wasToggleReleased = FALSE
                            tryingToUsePurge = FALSE
                        ENDIF
                    ENDIF
                ELSE
                    wasToggleReleased = TRUE
                ENDIF
            ENDIF

            IF nosToggle = TRUE
                RETURN_TRUE
            ELSE
                RETURN_FALSE
            ENDIF
        ELSE
            READ_STRUCT_OFFSET settings 32 4 iTemp0
            IF IS_KEY_PRESSED iTemp0
                READ_STRUCT_OFFSET settings 52 4 iTemp0
                IF iTemp0 = TRUE //Looking sideways
                    IF GOSUB IsLookingSideways
                        RETURN_FALSE
                    ELSE
                        RETURN_TRUE
                    ENDIF
                ELSE
                    RETURN_TRUE
                ENDIF
            ELSE
                RETURN_FALSE
            ENDIF
        ENDIF
    ENDIF
ENDIF

RETURN

IsPressingPurgeButton:
IF NOT IS_PC_USING_JOYPAD
    READ_STRUCT_OFFSET settings 52 4 iTemp0

    IF iTemp0 = TRUE
        IF GOSUB IsLookingSideways
            RETURN_FALSE
            RETURN
        ENDIF
    ENDIF
ENDIF

IF IS_PC_USING_JOYPAD
    IF wasToggleReleased = TRUE
        GET_CONTROLLER_MODE iTemp0
        IF iTemp0 = 0
            IF IS_BUTTON_PRESSED 0 CIRCLE
            //OR IS_BUTTON_PRESSED 0 LEFTSHOULDER1
                RETURN_TRUE
            ELSE
                RETURN_FALSE
            ENDIF
        ELSE
            IF IS_BUTTON_PRESSED 0 SQUARE
                RETURN_TRUE
            ELSE
                RETURN_FALSE
            ENDIF
        ENDIF
    ELSE
        RETURN_FALSE
    ENDIF
ELSE
    READ_STRUCT_OFFSET settings 32 4 iTemp0
    READ_STRUCT_OFFSET settings 60 4 iTemp1
    IF iTemp0 = iTemp1
        IF wasToggleReleased = TRUE
            READ_STRUCT_OFFSET settings 60 4 iTemp0
            IF IS_KEY_PRESSED iTemp0
                RETURN_TRUE
            ELSE
                RETURN_FALSE
            ENDIF
        ELSE
            RETURN_FALSE
        ENDIF
    ELSE
        READ_STRUCT_OFFSET settings 60 4 iTemp0
        IF IS_KEY_PRESSED iTemp0
            RETURN_TRUE
        ELSE
            RETURN_FALSE
        ENDIF
    ENDIF
ENDIF

RETURN

UsePurge:
WRITE_STRUCT_OFFSET vehPointer 0x8A4 4 1.0
READ_STRUCT_OFFSET settings 28 4 fTemp1
GET_AUDIO_SFX_VOLUME fTemp0
fTemp0 *= fTemp1
SET_AUDIO_STREAM_VOLUME audioStream fTemp0
IF usingPurge = FALSE
    PLAY_FX_SYSTEM fx0
    PLAY_FX_SYSTEM fx1
    usingPurge = TRUE
    SET_AUDIO_STREAM_STATE audioStream 1
ENDIF

GOSUB DecreaseNos

nosToggle = FALSE
wasToggleReleased = TRUE
tryingToUsePurge = FALSE

RETURN

StopNosUsage:
IF wasUsingNos = TRUE
    WRITE_STRUCT_OFFSET vehPointer 0x8A4 4 1.0
    GOSUB StopNitroPurge
    wasUsingNos = FALSE
    TIMERA = 0
ELSE
    READ_STRUCT_OFFSET settings 36 4 iTemp0 //ConsumableMode
    IF IS_CAR_ENGINE_ON veh
    AND iTemp0 = FALSE
        READ_STRUCT_OFFSET settings 16 4 iTemp0 //RefillDelay
        IF iTemp0 < TIMERA
            GOSUB RefillNos
        ENDIF
    ELSE
        TIMERA = 0
    ENDIF
ENDIF

nosToggle = FALSE

RETURN

UseNos:
WRITE_STRUCT_OFFSET vehPointer 0x8A4 4 -0.5
GOSUB StopNitroPurge
GOSUB DecreaseNos

TIMERB = 0

RETURN

CheckIfVehExists:
IF NOT DOES_VEHICLE_EXIST veh
    KILL_FX_SYSTEM fx0
    KILL_FX_SYSTEM fx1
    REMOVE_AUDIO_STREAM audioStream
    TERMINATE_THIS_CUSTOM_SCRIPT
ENDIF

RETURN

CheckIfNosExists:
GET_CURRENT_CAR_MOD veh 8 mod //This command was modified in the asi file

IF mod = -1
    KILL_FX_SYSTEM fx0
    KILL_FX_SYSTEM fx1
    REMOVE_AUDIO_STREAM audioStream
    TERMINATE_THIS_CUSTOM_SCRIPT
ENDIF

RETURN

IsOnGarage:
GET_SCRIPT_STRUCT_NAMED CARMOD iTemp0
GET_CHAR_AREA_VISIBLE scplayer iTemp1

IF iTemp0 = 0
OR iTemp1 = 0
    RETURN_FALSE
ELSE
    RETURN_TRUE
ENDIF

RETURN

StopNitroPurge:
IF usingPurge = TRUE
    STOP_FX_SYSTEM fx0
    STOP_FX_SYSTEM fx1
    usingPurge = FALSE
    SET_AUDIO_STREAM_STATE audioStream FALSE
ENDIF

RETURN

IsPlayerDriving:
IF IS_CHAR_SITTING_IN_ANY_CAR scplayer
    GET_CAR_CHAR_IS_USING scplayer iTemp0
    GET_DRIVER_OF_CAR iTemp0 iTemp1

    IF scplayer = iTemp1
        IF veh = iTemp0
            RETURN_TRUE
        ELSE
            RETURN_FALSE
        ENDIF
    ELSE
        RETURN_FALSE
    ENDIF
ELSE
    RETURN_FALSE
ENDIF

RETURN

SetNosAmount:
SWITCH mod
    CASE 1009
        WRITE_STRUCT_OFFSET vehPointer 0x48A 1 2
    BREAK
    CASE 1008
        WRITE_STRUCT_OFFSET vehPointer 0x48A 1 5
    BREAK
    CASE 1010
        WRITE_STRUCT_OFFSET vehPointer 0x48A 1 10
    BREAK
    DEFAULT
        WRITE_STRUCT_OFFSET vehPointer 0x48A 1 10
    BREAK
ENDSWITCH

RETURN

IsLookingSideways:
IF IS_PC_USING_JOYPAD
    GET_CONTROLLER_MODE iTemp0
    IF iTemp0 = 0
        IF IS_BUTTON_PRESSED 0 LEFTSHOULDER2
        OR IS_BUTTON_PRESSED 0 RIGHTSHOULDER2
            IF IS_BUTTON_PRESSED 0 LEFTSHOULDER2
            AND IS_BUTTON_PRESSED 0 RIGHTSHOULDER2
                RETURN_FALSE
            ELSE
                RETURN_TRUE
            ENDIF
        ELSE
            RETURN_FALSE
        ENDIF
    ELSE
        IF IS_BUTTON_PRESSED 0 LEFTSHOULDER1 
        OR IS_BUTTON_PRESSED 0 RIGHTSHOULDER1
            IF IS_BUTTON_PRESSED 0 LEFTSHOULDER1
            AND IS_BUTTON_PRESSED 0 RIGHTSHOULDER1
                RETURN_FALSE
            ELSE
                RETURN_TRUE
            ENDIF
        ELSE
            RETURN_FALSE
        ENDIF
    ENDIF
ELSE
    READ_STRUCT_OFFSET 0xB73458 10 2 iTemp0
    READ_STRUCT_OFFSET 0xB73458 14 2 iTemp1
    IF iTemp0 = 255
    OR iTemp1 = 255
        IF iTemp0 = 255
        AND iTemp1 = 255
            RETURN_FALSE
        ELSE
            RETURN_TRUE
        ENDIF
    ELSE
        RETURN_FALSE
    ENDIF
ENDIF

RETURN

IsAccelerating:
READ_STRUCT_OFFSET vehPointer 0x49C 4 fTemp0
IF fTemp0 = 0.0
    RETURN_FALSE
ELSE
    RETURN_TRUE
ENDIF

RETURN

DecreaseNos:
READ_STRUCT_OFFSET settings 20 4 iTemp0 //InfiniteNos
IF iTemp0 = FALSE
    READ_STRUCT_OFFSET settings 36 4 iTemp0 //ConsumableMode
    IF iTemp0 = FALSE
        SWITCH mod
            CASE 1009
                READ_STRUCT_OFFSET settings 0 4 fTemp0
            BREAK
            CASE 1008
                READ_STRUCT_OFFSET settings 4 4 fTemp0
            BREAK
            CASE 1010
                READ_STRUCT_OFFSET settings 8 4 fTemp0
            BREAK
            DEFAULT
                READ_STRUCT_OFFSET settings 8 4 fTemp0
            BREAK
        ENDSWITCH

        IF usingPurge = TRUE
            READ_STRUCT_OFFSET settings 44 4 fTemp1
            fTemp0 *= fTemp1
        ENDIF
        currentNos -=@ fTemp0
    ELSE
        READ_STRUCT_OFFSET settings 48 4 fTemp0
        IF usingPurge = TRUE
            READ_STRUCT_OFFSET settings 44 4 fTemp1
            fTemp0 *= fTemp1
        ENDIF
        currentNos -=@ fTemp0
    ENDIF
ENDIF

wasUsingNos = TRUE

RETURN

RefillNos:
READ_STRUCT_OFFSET settings 12 4 fTemp1

SWITCH mod
    CASE 1009
        READ_STRUCT_OFFSET settings 0 4 fTemp0
    BREAK
    CASE 1008
        READ_STRUCT_OFFSET settings 4 4 fTemp0
    BREAK
    CASE 1010
        READ_STRUCT_OFFSET settings 8 4 fTemp0
    BREAK
    DEFAULT
        READ_STRUCT_OFFSET settings 8 4 fTemp0
    BREAK
ENDSWITCH
fTemp0 *= fTemp1
currentNos +=@ fTemp0

RETURN

SCRIPT_END
}