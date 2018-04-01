package yaossg.mod.mana_craft.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import yaossg.mod.Util;
import yaossg.mod.mana_craft.Config;
import yaossg.mod.mana_craft.ManaCraft;

import java.util.Random;


public class ItemManaApple extends ItemFood {
    public ItemManaApple()
    {
        super(6,1.0f,false);
        this.setCreativeTab(ManaCraft.tabMana);
        this.setAlwaysEdible();
        this.setUnlocalizedName("mana_apple");

    }

    @Override
    protected void onFoodEaten(ItemStack stack, World worldIn, EntityPlayer player) {
        if(!worldIn.isRemote) {
            player.addExperience(20);
            player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 300, 1));
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 1200, 0));
            player.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 1200, 0));
        }
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 12;
    }

    private static final Random random = new Random();
    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer playerIn, EntityLivingBase target, EnumHand hand) {
        if(target instanceof EntityPig && hand == EnumHand.MAIN_HAND && Config.PES > 0) {
            if(playerIn.getServer() != null)
                playerIn.getServer().getPlayerList().sendMessage(new TextComponentTranslation("message.pig"));
            EntityPig pig = (EntityPig) target;
            playerIn.attackEntityFrom(new DamageSource("byPig")
                    .setDifficultyScaled().setExplosion().setMagicDamage().setFireDamage(), 16);
            ItemManaApple.appleExplosin(pig);
            Util.giveManaCraftAdvancement(playerIn, "pig_bomb");
            stack.shrink(1);
            return true;
        }
        return false;
    }
    public static void appleExplosin(EntityPig pig) {
        Util.createThenApplyExplosin(pig.world, pig, pig.getPosition(), Config.PES, true, true);
        for(int i = 0; i < Config.PES; ++i)
            pig.world.spawnEntity(new EntityLightningBolt(pig.world,
                    pig.posX + (random.nextFloat() - 0.5f) * Config.PES,
                    pig.posY + (random.nextFloat() - 0.5f) * Config.PES,
                    pig.posZ + (random.nextFloat() - 0.5f) * Config.PES, false));
    }
}
