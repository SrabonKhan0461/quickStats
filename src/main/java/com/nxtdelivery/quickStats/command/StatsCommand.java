package com.nxtdelivery.quickStats.command;

import java.util.ArrayList;
import java.util.List;

import com.nxtdelivery.quickStats.*;
import com.nxtdelivery.quickStats.api.*;
import com.nxtdelivery.quickStats.gui.GUIConfig;
import com.nxtdelivery.quickStats.gui.GUIStats;
import com.nxtdelivery.quickStats.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

public class StatsCommand implements ICommand {

	private final List aliases;
	public boolean returnRelease;
	private static Minecraft mc = Minecraft.getMinecraft();

	public StatsCommand() {
		aliases = new ArrayList();
		aliases.add("qsts");
		aliases.add("quickstats");
		aliases.add("qs");
	}

	@Override
	public int compareTo(ICommand o) {
		return 0;
	}

	@Override
	public String getCommandName() {
		return "quickStats";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "quickStats <>";
	}

	@Override
	public List getCommandAliases() {
		return this.aliases;
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		try {
			switch (args[0]) {
			case "configure":
			case "config":
			case "cfg":
				try {
					new TickDelay(() -> mc.displayGuiScreen((GuiScreen)GUIConfig.INSTANCE.gui()),1);
				} catch(Exception e) {
					if(GUIConfig.debugMode) {e.printStackTrace();}
				}
				break;
			case "reload":
				QuickStats.LOGGER.info("Reloading config and version checker...");
				sender.addChatMessage((IChatComponent) new ChatComponentText(
						EnumChatFormatting.DARK_GRAY + "[QuickStats] Reloading!"));
				GUIConfig.INSTANCE.initialize();
				QuickStats.updateCheck = UpdateChecker.updateNeeded(Reference.VERSION);
				sender.addChatMessage((IChatComponent) new ChatComponentText(EnumChatFormatting.DARK_GRAY
						+ "[QuickStats] Reloaded! Relog and check logs for more infomation."));
				Minecraft.getMinecraft().thePlayer.playSound("minecraft:random.successful_hit", 1.0F, 1.0F);
				break;
			case "api":
				GUIConfig.apiKey = args[1];
				GUIConfig.INSTANCE.markDirty();
	    		GUIConfig.INSTANCE.writeData();
				sender.addChatMessage((IChatComponent) new ChatComponentText(
						EnumChatFormatting.DARK_GRAY + ("[QuickStats] set your API key as: " + args[1] + ".")));
				break;
			case "testLoc":
				sender.addChatMessage((IChatComponent) new ChatComponentText(
						EnumChatFormatting.DARK_GRAY + "[QuickStats] Testing locraw function..."));
				LocrawUtil locrawUtil = new LocrawUtil();
				locrawUtil.regist();
				break;
			case "test":
				sender.addChatMessage((IChatComponent) new ChatComponentText(
						EnumChatFormatting.DARK_GRAY + "[QuickStats] Testing function..."));
				GUIStats guiTest = new GUIStats("nxtdaydelivery");
				break;
			case "testEntity":
				try {
					sender.addChatMessage((IChatComponent) new ChatComponentText(
							EnumChatFormatting.DARK_GRAY + "[QuickStats] Testing getEntity function..."));
					QuickStats.LOGGER.info(GetEntity.get(0).getName());
					sender.addChatMessage((IChatComponent) new ChatComponentText(
							EnumChatFormatting.DARK_GRAY + "[QuickStats] entity = " + GetEntity.get(0).getName()));
				} catch (Exception e) {
					QuickStats.LOGGER.info("entity = null");
					sender.addChatMessage((IChatComponent) new ChatComponentText(
							EnumChatFormatting.DARK_GRAY + "[QuickStats] entity = null"));
				}
				break;
			default:
				GUIStats gui = new GUIStats(args[0]);
				break;
			}
		} catch (Exception e) {
			sender.addChatMessage((IChatComponent) new ChatComponentText(EnumChatFormatting.DARK_GRAY
					+ "[QuickStats] Command menu (mod version " + Reference.VERSION + ")"));
			sender.addChatMessage((IChatComponent) new ChatComponentText(EnumChatFormatting.DARK_GRAY
					+ "[QuickStats] Command usage: /quickstats <name>, /quickstats configure, /quickstats reload, /quickstats api <api key>"));
		}
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		return true;
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
		return null;
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		return false;
	}

}
