package mods.tinker.tconstruct.tools;

import ic2.api.IBoxable;
import ic2.api.ICustomElectricItem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import mods.tinker.common.ToolMod;
import mods.tinker.tconstruct.AbilityHelper;
import mods.tinker.tconstruct.TConstruct;
import mods.tinker.tconstruct.TConstructRegistry;
import mods.tinker.tconstruct.TContent;
import mods.tinker.tconstruct.client.TProxyClient;
import mods.tinker.tconstruct.crafting.ToolBuilder;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** NBTTags
 * Main tag - InfiTool
 * @see ToolBuilder
 * 
 * Required:
 * Head: Base and render tag, above the handle
 * Handle: Base and render tag, bottom layer
 * 
 * Damage: Replacement for metadata
 * MaxDamage: ItemStacks only read setMaxDamage()
 * Broken: Represents whether the tool is broken (boolean)
 * Attack: How much damage a mob will take
 * MiningSpeed: The speed at which a tool mines
 * 
 * Others: 
 * Accessory: Base and tag, above head. Sword guards, binding, etc
 * Effects: Render tag, top layer. Fancy effects like moss or diamond edge.
 * Render order: Handle > Head > Accessory > Effect1 > Effect2 > Effect3
 * Unbreaking: Reinforced in-game, 10% chance to not use durability per level
 * Shoddy/Spiny: Mines faster or slower and does less or more attack.
 * 
 * Modifiers have their own tags.
 * @see ToolMod
 */

public abstract class ToolCore extends Item implements ICustomElectricItem, IBoxable
{
	Random random = new Random();
	String toolTexture;
	public int damageVsEntity;

	public ToolCore(int id, int baseDamage, String texture)
	{
		super(id);
		this.maxStackSize = 1;
		this.setMaxDamage(100);
		this.setUnlocalizedName("InfiTool");
		this.setCreativeTab(TConstruct.toolTab);
		toolTexture = texture;
		damageVsEntity = baseDamage;
		TConstructRegistry.addToolMapping(this);
	}

	/** Determines what type of heads the tool has.
	 * 0: no heads
	 * 1: one head
	 * 2: two heads
	 * 3: Two heads, different uses
	 * @return The head type
	 */
	public abstract int getHeadType ();

	public String getToolName ()
	{
		return this.getClass().getSimpleName();
	}

	/* Rendering */

	//public HashMap<Integer, String> partTextures = new HashMap<Integer, String>();
	//public HashMap<Integer, String> effectTextures = new HashMap<Integer, String>();
	public HashMap<Integer, Icon> headIcons = new HashMap<Integer, Icon>();
	public HashMap<Integer, Icon> brokenHeadIcons = new HashMap<Integer, Icon>();
	public HashMap<Integer, Icon> handleIcons = new HashMap<Integer, Icon>();
	public HashMap<Integer, Icon> accessoryIcons = new HashMap<Integer, Icon>();
	public HashMap<Integer, Icon> effectIcons = new HashMap<Integer, Icon>();

	//Not liking this
	public HashMap<Integer, String> headStrings = new HashMap<Integer, String>();
	public HashMap<Integer, String> brokenHeadStrings = new HashMap<Integer, String>();
	public HashMap<Integer, String> handleStrings = new HashMap<Integer, String>();
	public HashMap<Integer, String> accessoryStrings = new HashMap<Integer, String>();
	public HashMap<Integer, String> effectStrings = new HashMap<Integer, String>();

	@SideOnly(Side.CLIENT)
	@Override
	public boolean requiresMultipleRenderPasses ()
	{
		return true;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public int getRenderPasses (int metadata)
	{
		return 6;
	}

	//Override me please!
	public int getPartAmount ()
	{
		return 3;
	}

	public abstract String getIconSuffix (int partType);

	public abstract String getEffectSuffix ();

	public abstract String getDefaultFolder ();

	public void registerPartPaths (int index, String[] location)
	{
		headStrings.put(index, location[0]);
		brokenHeadStrings.put(index, location[1]);
		handleStrings.put(index, location[2]);
		accessoryStrings.put(index, location[3]);
	}

	public void registerEffectPath (int index, String location)
	{
		effectStrings.put(index, location);
	}

	@Override
	public void updateIcons (IconRegister iconRegister)
	{
		headIcons.clear();
		brokenHeadIcons.clear();
		handleIcons.clear();
		accessoryIcons.clear();
		effectIcons.clear();
		Iterator iter = headStrings.entrySet().iterator();
		while (iter.hasNext())
		{
			Map.Entry pairs = (Map.Entry) iter.next();
			headIcons.put((Integer) pairs.getKey(), iconRegister.registerIcon((String) pairs.getValue()));
		}

		iter = brokenHeadStrings.entrySet().iterator();
		while (iter.hasNext())
		{
			Map.Entry pairs = (Map.Entry) iter.next();
			brokenHeadIcons.put((Integer) pairs.getKey(), iconRegister.registerIcon((String) pairs.getValue()));
		}

		iter = handleStrings.entrySet().iterator();
		while (iter.hasNext())
		{
			Map.Entry pairs = (Map.Entry) iter.next();
			handleIcons.put((Integer) pairs.getKey(), iconRegister.registerIcon((String) pairs.getValue()));
		}

		if (getPartAmount() > 2)
		{
			iter = accessoryStrings.entrySet().iterator();
			while (iter.hasNext())
			{
				Map.Entry pairs = (Map.Entry) iter.next();
				accessoryIcons.put((Integer) pairs.getKey(), iconRegister.registerIcon((String) pairs.getValue()));
			}
		}

		iter = effectStrings.entrySet().iterator();
		while (iter.hasNext())
		{
			Map.Entry pairs = (Map.Entry) iter.next();
			effectIcons.put((Integer) pairs.getKey(), iconRegister.registerIcon((String) pairs.getValue()));
		}
	}

	@Override
	public Icon getIcon (ItemStack stack, int renderPass)
	{
		NBTTagCompound tags = stack.getTagCompound().getCompoundTag("InfiTool");

		if (renderPass < getPartAmount())
		{
			if (renderPass == 0) // Handle
			{
				return (handleIcons.get(tags.getInteger("RenderHandle")));
			}

			else if (renderPass == 1) // Head
			{
				if (tags.getBoolean("Broken"))
					return (brokenHeadIcons.get(tags.getInteger("RenderHead")));
				else
					return (headIcons.get(tags.getInteger("RenderHead")));
			}

			else if (renderPass == 2) // Accessory
			{
				return (accessoryIcons.get(tags.getInteger("RenderAccessory")));
			}
		}

		else
		{
			if (renderPass == getPartAmount())
			{
				if (tags.hasKey("Effect1"))
					return (effectIcons.get(tags.getInteger("Effect1")));
			}

			else if (renderPass == getPartAmount() + 1)
			{
				if (tags.hasKey("Effect2"))
					return (effectIcons.get(tags.getInteger("Effect2")));
			}

			else if (renderPass == getPartAmount() + 2)
			{
				if (tags.hasKey("Effect3"))
					return (effectIcons.get(tags.getInteger("Effect3")));
			}
		}

		return TProxyClient.blankSprite;
	}

	/*@SideOnly(Side.CLIENT)
	public String getIconLocation (ItemStack stack, int renderPass)
	{
		NBTTagCompound tags = stack.getTagCompound().getCompoundTag("InfiTool");

		if (renderPass == 0) // Handle
		{
			return (partTextures.get((Integer) tags.getInteger("RenderHandle")) + getRenderString(renderPass, false));
		}

		if (renderPass == 1) // Head
		{
			if (tags.getBoolean("Broken"))
				return (partTextures.get((Integer) tags.getInteger("RenderHead")) + getRenderString(renderPass, true));
			else
				return (partTextures.get((Integer) tags.getInteger("RenderHead")) + getRenderString(renderPass, false));
		}

		if (renderPass == 2) // Accessory
		{
			if (tags.hasKey("RenderAccessory"))
			{
				return (partTextures.get((Integer) tags.getInteger("RenderAccessory")) + getRenderString(renderPass, false));
			}
		}

		return TContent.blankSprite;
	}

	@SideOnly(Side.CLIENT)
	public String getEffectLocation (ItemStack stack, int renderPass)
	{
		NBTTagCompound tags = stack.getTagCompound().getCompoundTag("InfiTool");

		if (renderPass == 0)
		{
			if (tags.hasKey("Effect1"))
				return (effectTextures.get((Integer) tags.getInteger("Effect1")) + getEffectString(renderPass));
		}

		if (renderPass == 1)
		{
			if (tags.hasKey("Effect2"))
				return (effectTextures.get((Integer) tags.getInteger("Effect2")) + getEffectString(renderPass));
		}

		if (renderPass == 2)
		{
			if (tags.hasKey("Effect3"))
				return (effectTextures.get((Integer) tags.getInteger("Effect3")) + getEffectString(renderPass));
		}

		return TContent.blankSprite;
	}*/

	/* Tags and information about the tool */
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation (ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
		if (!stack.hasTagCompound())
			return;

		NBTTagCompound tags = stack.getTagCompound();
		if (tags.hasKey("charge"))
		{
			String color = "";
			//double joules = this.getJoules(stack);
			int power = tags.getInteger("charge");

			if (power != 0)
			{
				if (power <= this.getMaxCharge() / 3)
					color = "\u00a74";
				else if (power > this.getMaxCharge() * 2 / 3)
					color = "\u00a72";
				else
					color = "\u00a76";
			}

			String charge = new StringBuilder().append(color).append(tags.getInteger("charge")).append("/").append(getMaxCharge()).append(" En").toString();
			list.add(charge);
		}
		if (tags.hasKey("InfiTool"))
		{
			boolean broken = tags.getCompoundTag("InfiTool").getBoolean("Broken");
			if (broken)
				list.add("\u00A7oBroken");
			else
			{
				int head = tags.getCompoundTag("InfiTool").getInteger("Head") + 1;
				int handle = tags.getCompoundTag("InfiTool").getInteger("Handle") + 1;
				int binding = tags.getCompoundTag("InfiTool").getInteger("Accessory") + 1;

				String headName = getAbilityNameForType(head);
				if (!headName.equals(""))
					list.add(getColorCodeForType(head) + headName);

				String handleName = getAbilityNameForType(handle);
				if (!handleName.equals("") && handle != head)
					list.add(getColorCodeForType(handle) + handleName);

				String bindingName = getAbilityNameForType(binding);
				if (!bindingName.equals("") && binding != head && binding != handle)
					list.add(getColorCodeForType(binding) + bindingName);

				String reinforced = getReinforcedName(head, handle, binding);
				if (!reinforced.equals(""))
					list.add(reinforced);

				boolean displayToolTips = true;
				int tipNum = 0;
				while (displayToolTips)
				{
					tipNum++;
					String tooltip = "Tooltip" + tipNum;
					if (tags.getCompoundTag("InfiTool").hasKey(tooltip))
					{
						String tipName = tags.getCompoundTag("InfiTool").getString(tooltip);
						if (!tipName.equals(""))
							list.add(tipName);
					}
					else
						displayToolTips = false;
				}
			}
		}
	}

	public static String getColorCodeForType (int type) //TODO: unhardcode this
	{
		String colorCode = "\u00A7";
		switch (type)
		{
		case 1:
			return colorCode + "e"; //Wood
		case 2:
			return ""; //Stone
		case 3:
			return colorCode + "f"; //Iron
		case 4:
			return colorCode + "8"; //Flint
		case 5:
			return colorCode + "2"; //Cactus
		case 6:
			return colorCode + "e"; //Bone
		case 7:
			return colorCode + "d"; //Obsidian
		case 8:
			return colorCode + "4"; //Netherrack
		case 9:
			return colorCode + "a"; //Slime
		case 10:
			return colorCode + "f"; //Paper
		case 11:
			return colorCode + "3"; //Cobalt
		case 12:
			return colorCode + "4"; //Ardite
		case 13:
			return colorCode + "5"; //Manyullyn
		case 14:
			return colorCode + "c"; //Copper
		case 15:
			return colorCode + "6"; //Bronze
		}
		return colorCode;
	}

	public String getAbilityNameForType (int type)//TODO: unhardcode this
	{
		switch (type)
		{
		case 1:
			return ""; //Wood
		case 2:
			return "Shoddy"; //Stone
		case 3:
			return ""; //Iron
		case 4:
			return "Shoddy"; //Flint
		case 5:
			return "Spiny"; //Cactus
		case 6:
			return ""; //Bone
		case 7:
			return ""; //Obsidian
		case 8:
			return "Shoddy"; //Netherrack
		case 9:
			return ""; //Slime
		case 10:
			return "Writable"; //Paper
		case 11:
			return ""; //Cobalt
		case 12:
			return ""; //Ardite
		case 13:
			return "Awareness"; //Manyullyn
		case 14:
			return ""; //Copper
		case 15:
			return ""; //Bronze
		default:
			return "";
		}
	}

	//This method is temporary
	public static String getReinforcedName (int head, int handle, int accessory) //TODO: unhardcode this
	{
		if (head == 7 || handle == 7 || accessory == 7)
			return "\u00A7dReinforced III";
		else if (head == 11 || handle == 11 || accessory == 11)
			return "\u00A73Reinforced II";
		else if (head == 3 || handle == 3 || accessory == 3)
			return "\u00A7fReinforced I";
		else if (head == 15 || handle == 15 || accessory == 15)
			return "\u00A76Reinforced I";
		return "";
	}

	static String[] toolMaterialNames = { "Wooden ", "Stone ", "Iron ", "Flint ", "Cactus ", "Bone ", "Obsidian ", "Nethrrack ", "Slime ", "Paper ", "Cobalt ", "Ardite ", "Manyullyn ", "Copper ", "Bronze ", "Alumite ", "Steel " };

	/* Creative mode tools */
	public void getSubItems (int id, CreativeTabs tab, List list)
	{
		for (int i = 0; i < 17; i++)
		{
			Item accessory = getAccessoryItem();
			ItemStack accessoryStack = accessory != null ? new ItemStack(getAccessoryItem(), 1, i) : null;
			ItemStack tool = ToolBuilder.instance.buildTool(new ItemStack(getHeadItem(), 1, i), new ItemStack(getHandleItem(), 1, i), accessoryStack, toolMaterialNames[i] + getToolName());
			tool.getTagCompound().getCompoundTag("InfiTool").setBoolean("Built", true);
			list.add(tool);
		}
	}

	protected abstract Item getHeadItem ();

	protected abstract Item getAccessoryItem ();

	protected Item getHandleItem ()
	{
		return TContent.toolRod;
	}

	public void onUpdate (ItemStack stack, World world, Entity entity, int par4, boolean par5)
	{
		if (entity instanceof EntityLiving)
		{
			NBTTagCompound tags = stack.getTagCompound().getCompoundTag("InfiTool");
			if (tags.hasKey("Moss"))
			{
				int chance = tags.getInteger("Moss");
				if (random.nextInt(2000) < chance)
				{
					AbilityHelper.damageTool(stack, -1, (EntityLiving) entity, true);
				}
			}
		}
	}

	/* Tool uses */
	@Override
	public boolean onBlockStartBreak (ItemStack stack, int x, int y, int z, EntityPlayer player)
	{
		NBTTagCompound tags = stack.getTagCompound().getCompoundTag("InfiTool");
		World world = player.worldObj;
		int bID = player.worldObj.getBlockId(x, y, z);
		int meta = world.getBlockMetadata(x, y, z);
		Block block = Block.blocksList[bID];

		if (tags.getBoolean("Lava") && block.quantityDropped(meta, 0, random) != 0)
		{
			ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(new ItemStack(block.idDropped(bID, random, 0), 1, block.damageDropped(meta)));
			if (result != null)
			{
				world.setBlockToAir(x, y, z);
				if (!player.capabilities.isCreativeMode)
					onBlockDestroyed(stack, world, bID, x, y, z, player);
				if (!world.isRemote)
				{
					EntityItem entityitem = new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, result.copy());

					entityitem.delayBeforeCanPickup = 10;
					world.spawnEntityInWorld(entityitem);
					world.playAuxSFX(2001, x, y, z, bID + (meta << 12));
				}
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean onBlockDestroyed (ItemStack itemstack, World world, int bID, int x, int y, int z, EntityLiving player)
	{
		return AbilityHelper.onBlockChanged(itemstack, world, bID, x, y, z, player, random);
	}

	@Override
	public boolean hitEntity (ItemStack stack, EntityLiving mob, EntityLiving player)
	{
		//AbilityHelper.hitEntity(stack, mob, player);
		//AbilityHelper.damageTool(stack, 1, player, false);
		return true;
	}

	@Override
	public float getStrVsBlock (ItemStack stack, Block block, int meta)
	{
		NBTTagCompound tags = stack.getTagCompound();
		if (tags.getCompoundTag("InfiTool").getBoolean("Broken"))
			return 0.1f;
		return 1f;
	}

	//Vanilla repairs
	public boolean isItemTool (ItemStack par1ItemStack)
	{
		return false;
	}

	@Override
	public boolean getIsRepairable (ItemStack par1ItemStack, ItemStack par2ItemStack)
	{
		return false;
	}

	/* Attacking */
	@Override
	public boolean onLeftClickEntity (ItemStack stack, EntityPlayer player, Entity entity)
	{
		AbilityHelper.onLeftClickEntity(stack, player, entity, this);
		return true;
	}

	public boolean pierceArmor ()
	{
		return false;
	}

	public float chargeAttack ()
	{
		return 1f;
	}

	public int getDamageVsEntity (Entity par1Entity) //Redundant
	{
		return this.damageVsEntity;
	}

	/* Enchanting */
	public int getItemEnchantability ()
	{
		return 0;
	}

	//Changes how much durability the base tool has
	public float getDurabilityModifier ()
	{
		return 1f;
	}

	public boolean isFull3D ()
	{
		return true;
	}

	/*
	 * IC2 Support
	 * Every tool can be an electric tool if you modify it right
	 */
	@Override
	public boolean canBeStoredInToolbox (ItemStack itemstack)
	{
		return true;
	}

	@Override
	public boolean canProvideEnergy ()
	{
		return true;
	}

	@Override
	public int getChargedItemId ()
	{
		return this.itemID;
	}

	@Override
	public int getEmptyItemId ()
	{
		return this.itemID;
	}

	@Override
	public int getMaxCharge ()
	{
		return 10000;
	}

	@Override
	public int getTier ()
	{
		return 0;
	}

	@Override
	public int getTransferLimit ()
	{
		return 32;
	}

	@Override
	public int charge (ItemStack stack, int amount, int tier, boolean ignoreTransferLimit, boolean simulate)
	{
		NBTTagCompound tags = stack.getTagCompound();
		if (!tags.hasKey("charge")) //|| !tags.getCompoundTag("InfiTool").getBoolean("Electric"))
			return 0;

		if (amount > 0)
		{
			if (amount > getTransferLimit() && !ignoreTransferLimit)
			{
				amount = getTransferLimit();
			}

			int charge = tags.getInteger("charge");

			if (amount > getMaxCharge() - charge)
			{
				amount = getMaxCharge() - charge;
			}

			charge += amount;

			if (!simulate)
			{
				tags.setInteger("charge", charge);
				stack.setItemDamage(1 + (getMaxCharge() - charge) * (stack.getMaxDamage() - 2) / getMaxCharge());
			}

			return amount;
		}

		else
			return 0;
	}

	@Override
	public int discharge (ItemStack stack, int amount, int tier, boolean ignoreTransferLimit, boolean simulate)
	{
		NBTTagCompound tags = stack.getTagCompound();
		if (!tags.hasKey("charge"))// || !tags.getCompoundTag("InfiTool").getBoolean("Electric"))
			return 0;

		if (amount > 0)
		{
			if (amount > getTransferLimit() && !ignoreTransferLimit)
			{
				amount = getTransferLimit();
			}

			int charge = tags.getInteger("charge");

			if (amount > charge)
			{
				amount = charge;
			}

			charge -= amount;

			if (!simulate)
			{
				tags.setInteger("charge", charge);
				stack.setItemDamage(1 + (getMaxCharge() - charge) * (stack.getMaxDamage() - 1) / getMaxCharge());
			}

			return amount;
		}

		else
			return 0;
	}

	@Override
	public boolean canUse (ItemStack itemStack, int amount)
	{
		return false;
	}

	@Override
	public boolean canShowChargeToolTip (ItemStack itemStack)
	{
		return false;
	}

	/* Universal Electricity Support 
	 * Disabled due to Calclavia changing UE's api too often
	 */

	/*@Override
	public double getJoules (Object... data)
	{
		if (data[0] instanceof ItemStack)
		{
			ItemStack itemStack = (ItemStack) data[0];

			if (itemStack.stackTagCompound == null) { return 0; }
			int electricityStored = itemStack.stackTagCompound.getInteger("charge");
			//itemStack.setItemDamage((int) (getMaxJoules(itemStack) - electricityStored));
			itemStack.setItemDamage((int) (1 + (getMaxCharge() - electricityStored) * (itemStack.getMaxDamage() - 1) / getMaxCharge()));
			return electricityStored;
		}

		return -1;
	}

	@Override
	public void setJoules (double joules, Object... data)
	{
		if (data[0] instanceof ItemStack)
		{
			ItemStack itemStack = (ItemStack) data[0];

			if (itemStack.stackTagCompound == null)
			{
				itemStack.setTagCompound(new NBTTagCompound());
			}

			double electricityStored = ( Math.max(Math.min(joules, this.getMaxJoules(itemStack)), 0) );
			itemStack.stackTagCompound.setInteger("charge", (int) electricityStored);
			//itemStack.setItemDamage((int) (getMaxJoules() - electricityStored));
			itemStack.setItemDamage((int) (1 + (getMaxCharge() - electricityStored) * (itemStack.getMaxDamage() - 1) / getMaxCharge()));
		}
	}

	@Override
	public double getMaxJoules (Object... data)
	{
		return 10000;
	}

	@Override
	public double getVoltage ()
	{
		return 120;
	}

	@Override
	public double onReceive (double amps, double voltage, ItemStack itemStack)
	{
		double rejectedElectricity = Math.max((this.getJoules(itemStack) + this.getJoules(amps, voltage, 1)) - this.getMaxJoules(itemStack), 0);
		this.setJoules(this.getJoules(itemStack) + this.getJoules(amps, voltage, 1) - rejectedElectricity, itemStack);
		return rejectedElectricity;
	}
	
	public static double getJoules(double amps, double voltage, double seconds)
	{
		return amps * voltage * seconds;
	}

	@Override
	public double onUse (double joulesNeeded, ItemStack itemStack)
	{
		double electricityToUse = Math.min(this.getJoules(itemStack), joulesNeeded);
		this.setJoules(this.getJoules(itemStack) - electricityToUse, itemStack);
		return electricityToUse;
	}

	@Override
	public boolean canReceiveElectricity ()
	{
		return true;
	}

	@Override
	public boolean canProduceElectricity ()
	{
		return true;
	}*/
}
