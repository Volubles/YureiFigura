package org.figuramc.figura.utils;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLKeyboard;

/** vanilla switched to sdl. lua still gets the old key ids and modifier bits. */
public final class InputUtils {
    private InputUtils() {
    }

    public static int modifiers() {
        return modifiers(SDLKeyboard.SDL_GetModState());
    }

    public static int modifiers(int sdl) {
        int legacy = 0;
        if ((sdl & InputConstants.MOD_SHIFT) != 0) {
            legacy |= 1;
        }
        if ((sdl & InputConstants.MOD_CONTROL) != 0) {
            legacy |= 2;
        }
        if ((sdl & InputConstants.MOD_ALT) != 0) {
            legacy |= 4;
        }
        if ((sdl & InputConstants.MOD_SUPER) != 0) {
            legacy |= 8;
        }
        if ((sdl & InputConstants.MOD_CAPS_LOCK) != 0) {
            legacy |= 16;
        }
        if ((sdl & InputConstants.MOD_NUM_LOCK) != 0) {
            legacy |= 32;
        }
        return legacy;
    }

    public static int mouseButton(int sdl) {
        return switch (sdl) {
            case 1 -> 0;
            case 2 -> 2;
            case 3 -> 1;
            default -> sdl - 1;
        };
    }

    public static int keyAction(int sdl) {
        return sdl == InputConstants.REPEAT ? 2 : sdl;
    }

    public static int keyId(InputConstants.Key key) {
        return key.getType() == InputConstants.Type.MOUSE ? mouseButton(key.getValue()) : keyId(key.getValue());
    }

    public static int keyId(int scanCode) {
        if (scanCode >= 4 && scanCode <= 29) {
            return scanCode - 4 + 65;
        }
        if (scanCode >= 30 && scanCode <= 38) {
            return scanCode - 30 + 49;
        }
        if (scanCode >= 58 && scanCode <= 69) {
            return scanCode - 58 + 290;
        }
        if (scanCode >= 104 && scanCode <= 115) {
            return scanCode - 104 + 302;
        }
        if (scanCode >= 89 && scanCode <= 97) {
            return scanCode - 89 + 321;
        }
        return switch (scanCode) {
            case 39 -> 48;
            case 40 -> 257;
            case 41 -> 256;
            case 42 -> 259;
            case 43 -> 258;
            case 44 -> 32;
            case 45 -> 45;
            case 46 -> 61;
            case 47 -> 91;
            case 48 -> 93;
            case 49 -> 92;
            case 50 -> 162;
            case 51 -> 59;
            case 52 -> 39;
            case 53 -> 96;
            case 54 -> 44;
            case 55 -> 46;
            case 56 -> 47;
            case 57 -> 280;
            case 70 -> 283;
            case 71 -> 281;
            case 72 -> 284;
            case 73 -> 260;
            case 74 -> 268;
            case 75 -> 266;
            case 76 -> 261;
            case 77 -> 269;
            case 78 -> 267;
            case 79 -> 262;
            case 80 -> 263;
            case 81 -> 264;
            case 82 -> 265;
            case 83 -> 282;
            case 84 -> 331;
            case 85 -> 332;
            case 86 -> 333;
            case 87 -> 334;
            case 88 -> 335;
            case 98 -> 320;
            case 99, 220 -> 330;
            case 100 -> 161;
            case 103 -> 336;
            case 101, 118 -> 348;
            case 224 -> 341;
            case 225 -> 340;
            case 226 -> 342;
            case 227 -> 343;
            case 228 -> 345;
            case 229 -> 344;
            case 230 -> 346;
            case 231 -> 347;
            default -> -1;
        };
    }
}
