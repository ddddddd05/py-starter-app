package com.pystarter.app.game;

import java.util.HashMap;
import java.util.Map;

/**
 * 所有像素图案。每个字母代表一种颜色（见 base()），'.' 是透明。
 * 同一个图案换一套颜色（换色）就能变成不同章节的怪物，例如绿色史莱姆 → 紫色蝙蝠色调。
 */
public final class Sprites {

    public static final class Sprite {
        public final String[] rows;
        public final Map<Character, Integer> palette;

        Sprite(String[] rows, Map<Character, Integer> palette) {
            this.rows = rows;
            this.palette = palette;
        }
    }

    private Sprites() {}

    private static Map<Character, Integer> base() {
        Map<Character, Integer> p = new HashMap<>();
        p.put('K', 0xFF000000);   // 描边
        p.put('W', 0xFFFFF1E8);   // 白
        p.put('S', 0xFFFFCCAA);   // 皮肤
        p.put('Y', 0xFFFFEC27);   // 黄
        p.put('O', 0xFFFFA300);   // 橙
        p.put('R', 0xFFFF004D);   // 红
        p.put('B', 0xFF29ADFF);   // 蓝
        p.put('G', 0xFF00E436);   // 绿
        p.put('D', 0xFF008751);   // 深绿
        p.put('P', 0xFF7E2553);   // 紫红
        p.put('L', 0xFFC2C3C7);   // 浅灰
        p.put('A', 0xFF5F574F);   // 深灰
        p.put('N', 0xFFAB5236);   // 棕
        p.put('V', 0xFF83769C);   // 淡紫
        return p;
    }

    /** 把图案里的“主色”换成别的颜色。 */
    private static Map<Character, Integer> swap(char key, int color) {
        Map<Character, Integer> p = base();
        p.put(key, color);
        return p;
    }

    // ---------- 主角：小骑士 ----------
    private static final String[] HERO = {
            "....KKKK....",
            "...KBBBBK...",
            "..KBBBBBBK..",
            "..KSSSSSSK..",
            "..KSKSSKSK..",
            "..KSSSSSSK.L",
            "...KKBBKK..L",
            "..KBBBBBBK.L",
            ".KSKBYYBKSKN",
            "...KBBBBK...",
            "...KNK.KNK..",
            "...KKK.KKK..",
    };

    // ---------- NPC：讲解知识的小法师 ----------
    private static final String[] WIZARD = {
            ".....KK.....",
            "....KVVK....",
            "...KVVYVK...",
            "..KVVVVVVK..",
            ".KKKKKKKKKK.",
            "...KSSSSK...",
            "...KSKKSK...",
            "...KWWWWK...",
            "..KVWWWWVK..",
            ".KVVVWWVVVK.",
            ".KVVVVVVVVK.",
            "..KKKKKKKK..",
    };

    // ---------- 小怪：史莱姆（换色后用在各章） ----------
    private static final String[] SLIME = {
            "............",
            "............",
            "....KKKK....",
            "...KGGGGK...",
            "..KGWGGGGK..",
            ".KGGGGGGGGK.",
            ".KGKGGGGKGK.",
            ".KGKGGGGKGK.",
            "KGGGGGGGGGGK",
            "KGGGGKKGGGGK",
            "KGGGGGGGGGGK",
            ".KKKKKKKKKK.",
    };

    // ---------- 精英：幽灵 ----------
    private static final String[] GHOST = {
            "...KKKKKK...",
            "..KWWWWWWK..",
            ".KWWWWWWWWK.",
            ".KWKKWWKKWK.",
            ".KWKRWWKRWK.",
            ".KWWWWWWWWK.",
            ".KWWWKKWWWK.",
            ".KWWWWWWWWK.",
            ".KWWWWWWWWK.",
            ".KWWWWWWWWK.",
            ".KWKWWKWWKWK",
            "..K.KK.KK.K.",
    };

    // ---------- Boss：小龙 ----------
    private static final String[] DRAGON = {
            "..KK........KK..",
            ".KRRK......KRRK.",
            ".KRRRKKKKKKRRRK.",
            "..KRRRRRRRRRRK..",
            "..KRRYKRRKYRRK..",
            "..KRRRRRRRRRRK..",
            "...KRRWKKWRRK...",
            "...KRRRRRRRRK...",
            "..KKRRRRRRRRKK..",
            ".KRRKRROORRKRRK.",
            "KRRRKRROORRKRRRK",
            "KRRKKRROORRKKRRK",
            ".KK.KRRRRRRK.KK.",
            "....KRRKKRRK....",
            "....KRK..KRK....",
            "....KKK..KKK....",
    };

    // ---------- 地图上的宝箱（全部通关后显示） ----------
    private static final String[] CHEST = {
            "............",
            "..KKKKKKKK..",
            ".KNNNNNNNNK.",
            ".KNNNNNNNNK.",
            "KKKKKKKKKKKK",
            "KNNNNYYNNNNK",
            "KNNNNYKNNNNK",
            "KNNNNNNNNNNK",
            "KNNNNNNNNNNK",
            "KKKKKKKKKKKK",
            "............",
            "............",
    };

    public static Sprite hero() {
        return new Sprite(HERO, base());
    }

    public static Sprite wizard() {
        return new Sprite(WIZARD, base());
    }

    public static Sprite chest() {
        return new Sprite(CHEST, base());
    }

    /** 各章敌人的颜色：新手村偏绿，森林偏紫，城堡偏灰黑。 */
    private static int themeColor(String theme, int stage) {
        switch (theme) {
            case "forest":
                return stage == 0 ? 0xFF83769C : stage == 1 ? 0xFF00E436 : 0xFFAB5236;
            case "castle":
                return stage == 0 ? 0xFFC2C3C7 : stage == 1 ? 0xFF29ADFF : 0xFF5F574F;
            default:
                return stage == 0 ? 0xFF00E436 : stage == 1 ? 0xFFFFCCAA : 0xFFFF004D;
        }
    }

    /** stage：0 小怪，1 精英，2 Boss；复仇怪用 stage 3（红色幽灵）。 */
    public static Sprite enemy(String theme, int stage) {
        switch (stage) {
            case 0:
                return new Sprite(SLIME, swap('G', themeColor(theme, 0)));
            case 1:
                return new Sprite(GHOST, swap('W', themeColor(theme, 1)));
            case 2:
                return new Sprite(DRAGON, swap('R', themeColor(theme, 2)));
            default:
                return new Sprite(GHOST, swap('W', 0xFFFF77A8));
        }
    }
}
