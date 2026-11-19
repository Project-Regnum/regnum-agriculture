package vectorwing.farmersdelight.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameRules;

public class NourishmentEffect extends MobEffect
{

	public NourishmentEffect() {
		super(MobEffectCategory.BENEFICIAL, 0xF3B300);
	}

	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity.getCommandSenderWorld().isClientSide) {
			return true;
		}

		if (entity instanceof Player player) {
			FoodData foodData = player.getFoodData();
			boolean isPlayerHealingWithSaturation =
					player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)
					&& player.isHurt()
					&& foodData.getSaturationLevel() > 0.0;
			if (!isPlayerHealingWithSaturation) {
				foodData.setExhaustion(0);
			}
		}

		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
