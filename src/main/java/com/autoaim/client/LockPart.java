package com.autoaim.client;

/**
 * 锁定部位：决定自动瞄准时把准心对到实体的哪一点。
 * <ul>
 *   <li>{@link #CENTER}：碰撞箱中心（脚底 + 高度 / 2），即 1.1.0 之前的原行为；</li>
 *   <li>{@link #HEAD}：眼睛高度（+ 可调微调）。TaCZ（永恒枪械工坊：零）的爆头判定默认是
 *       「命中点离脚底的高度落在眼睛高度 ±0.25 格内」，因此眼睛高度正好是判定带的正中心。</li>
 * </ul>
 * 本枚举刻意不引用任何 Minecraft 类，便于与 {@link AimController} 一起做 JUnit 单元测试。
 */
public enum LockPart {
    CENTER("autoaim.lock_part.center"),
    HEAD("autoaim.lock_part.head");

    private final String translationKey;

    LockPart(String translationKey) {
        this.translationKey = translationKey;
    }

    /** 语言文件里显示名对应的键（准星下方提示、配置界面按钮共用）。 */
    public String translationKey() {
        return translationKey;
    }

    /** 下一个模式，供配置界面按钮循环切换；以后新增模式也不必改界面代码。 */
    public LockPart next() {
        LockPart[] all = values();
        return all[(this.ordinal() + 1) % all.length];
    }
}
