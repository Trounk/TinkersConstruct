package mods.tinker.tconstruct.items;

import java.util.List;

import mods.tinker.tconstruct.TConstruct;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.util.MathHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class CraftingItem extends Item
{
	public String[] textureNames;
	public String[] unlocalizedNames;
	public String folder;
	public Icon[] icons;
	public CraftingItem(int id, String[] names, String[] tex, String f)
	{
		super(id);
		this.setCreativeTab(TConstruct.materialTab);
		this.setMaxDamage(0);
		this.setHasSubtypes(true);
		textureNames = tex;
		unlocalizedNames = names;
		folder = f;
	}

	@SideOnly(Side.CLIENT)
	public Icon getIconFromDamage(int meta)
	{
		return icons[meta];
	}
	
	@SideOnly(Side.CLIENT)
    public void updateIcons(IconRegister iconRegister)
    {
		this.icons = new Icon[textureNames.length];

        for (int i = 0; i < this.icons.length; ++i)
        {
            this.icons[i] = iconRegister.registerIcon("tinker:"+folder+textureNames[i]);
        }
    }
	
	public String getUnlocalizedName(ItemStack stack)
	{
		int arr = MathHelper.clamp_int(stack.getItemDamage(), 0, unlocalizedNames.length);
		return getUnlocalizedName() + "." +unlocalizedNames[arr];
	}

	public void getSubItems (int id, CreativeTabs tab, List list)
	{
		for (int i = 0; i < unlocalizedNames.length; i++)
			list.add(new ItemStack(id, 1, i));
	}
}
