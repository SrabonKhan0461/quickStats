/* Changelog v1.6.3
 *  fully added hash checker
 *  bug fixes
 *  code cleanup
 */

package com.nxtdelivery.quickStats;

import com.nxtdelivery.quickStats.command.StatsCommand;
import com.nxtdelivery.quickStats.gui.GUIConfig;
import com.nxtdelivery.quickStats.gui.GUIStats;
import com.nxtdelivery.quickStats.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.File;

@Mod(modid = Reference.MODID, name = Reference.NAME, version = Reference.VERSION)
public class QuickStats {

    @Mod.Instance("qSts")
    public static QuickStats instance;
    private static final Minecraft mc = Minecraft.getMinecraft();
    private KeyBinding statsKey;
    public static final Logger LOGGER = LogManager.getLogger(Reference.NAME);
    public static File JarFile;
    public static boolean updateCheck;
    public static boolean betaFlag = true;
    public static boolean locraw = false;
    public static boolean corrupt = false;
    public static LocrawUtil LocInst;
    public static GUIStats GuiInst;
    public static boolean onHypixel = false;
    boolean set = false;
    String partySet;

    @EventHandler()
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("preloading config...");
        try {
            GUIConfig.INSTANCE.preload();
            LOGGER.info("config preload was successful");
        } catch (Exception e) {
            if (GUIConfig.debugMode) {
                e.printStackTrace();
            }
            corrupt = true;
            LOGGER.error("Config failed to read. File has been reset. If you just reset your config, ignore this message.");
        }
        JarFile = event.getSourceFile();
        if (GUIConfig.debugMode) {
            LOGGER.info("Got JAR File: " + JarFile.getPath());
        }
    }

    @EventHandler()
    public void init(FMLInitializationEvent event) {
        LOGGER.info("attempting to check update status and mod authenticity...");
        updateCheck = UpdateChecker.checkUpdate(Reference.VERSION);
        AuthChecker.checkAuth(JarFile.getPath());
        LOGGER.info("registering settings...");
        statsKey = new KeyBinding("Get Stats", GUIConfig.key, "QuickStats");
        ClientRegistry.registerKeyBinding(statsKey);
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new StatsCommand());
        LocInst = new LocrawUtil();
        GuiInst = new GUIStats();
        locraw = true;
        LOGGER.debug(instance.toString());        // please stop moaning at me intellij
        LOGGER.info("Complete! QuickStats loaded successfully.");
    }

    @SubscribeEvent
    public void onInputEvent(InputEvent event) {
        if (onHypixel || GUIConfig.otherServer) {
            int keyCode;

            if (event instanceof InputEvent.MouseInputEvent) {
                keyCode = ((InputEvent.MouseInputEvent) event).getButton() + 100; // Adding 100 to distinguish mouse buttons
            } else {
                keyCode = Keyboard.getEventKey();
            }

            if (keyCode == statsKey.getKeyCode() && GUIConfig.modEnabled) {
                if (GUIConfig.key != keyCode) {
                    if (Keyboard.getEventKeyState()) {
                        try {
                            Entity entity = GetEntity.get(0);
                            if (entity instanceof EntityPlayer) {
                                if (entity.getName() == null || entity.getName().equals("")) {
                                    return;
                                }
                                if (onHypixel) {
                                    if (entity.getDisplayName().getUnformattedText().startsWith("\u00A78[NPC]") || !entity.getDisplayName().getUnformattedText().startsWith("\u00A7")) {
                                        return;
                                    }
                                }
                                GuiInst.showGUI(entity.getName());
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
    }
// ... (Previous code remains unchanged)

    @SubscribeEvent
    public void onChatReceive(ClientChatReceivedEvent event) {
        if (onHypixel) {
            if (GUIConfig.autoGetAPI) {
                try {
                    if (event.message.getUnformattedText().contains("Your new API key is")) {
                        String apiMessage = event.message.getUnformattedText();
                        String apiKey = apiMessage.substring(20);
                        LOGGER.info("got API key from message: " + apiKey + ". writing and reloading config...");
                        GUIConfig.apiKey = apiKey;
                        GUIConfig.INSTANCE.markDirty();
                        GUIConfig.INSTANCE.writeData();
                        new TickDelay(() -> mc.thePlayer.addChatMessage(new ChatComponentText(
                                Reference.COLOR + "[QuickStats] Grabbed and set your API key. The mod is now ready to use!")),
                                5);
                        mc.thePlayer.playSound("minecraft:random.successful_hit", 1.0F, 1.0F);
                    }
                } catch (Exception e) {
                    if (GUIConfig.debugMode) {
                        e.printStackTrace();
                    }
                }
            }

            if (GUIConfig.doPartyDetection) {
                if (QuickStats.locraw) {
                    QuickStats.locraw = false;
                    LocInst.send();
                }
                if (LocrawUtil.lobby) {
                    try {
                        if (event.message.getUnformattedText().contains("Party ") || event.message.getUnformattedText().contains("lobby!")) {
                            return;
                        }
                        if (event.message.getUnformattedText().contains(mc.thePlayer.getName())) {
                            String username = getUsernameFromChat(event.message.getUnformattedText());
                            if (!username.equalsIgnoreCase(mc.thePlayer.getName())) {
                                event.setCanceled(true);
                                StringBuilder sb = new StringBuilder(event.message.getUnformattedText());
                                sb.insert(event.message.getUnformattedText().indexOf(mc.thePlayer.getName()), "\u00A7l");
                                mc.thePlayer.addChatMessage(new ChatComponentText(sb.toString()));
                                GuiInst.showGUI(username);
                            }
                        }

                        if (GUIConfig.doPartyDetectionPLUS) {
                            if (event.message.getUnformattedText().contains("say")) {
                                if (getUsernameFromChat(event.message.getUnformattedText()).equals(mc.thePlayer.getName())) {
                                    try {
                                        String unformatted = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
                                        partySet = StringUtils.substringAfter(unformatted, "say ");
                                        set = true;
                                        if (partySet.contains("my name")) {
                                            partySet = null;
                                            set = false;
                                        }
                                        if (GUIConfig.debugMode) {
                                            LOGGER.info(partySet);
                                        }
                                    } catch (Exception e) {
                                        if (GUIConfig.debugMode) {
                                            e.printStackTrace();
                                            set = false;
                                        }
                                    }
                                }
                                if (set) {
                                    if (event.message.getUnformattedText().contains(partySet)) {
                                        String username = getUsernameFromChat(event.message.getUnformattedText());
                                        if (!username.equalsIgnoreCase(mc.thePlayer.getName())) {
                                            event.setCanceled(true);
                                            StringBuilder sb = new StringBuilder(event.message.getUnformattedText());
                                            sb.insert(event.message.getUnformattedText().indexOf(partySet), "\u00A7l");
                                            mc.thePlayer.addChatMessage(new ChatComponentText(sb.toString()));
                                            GuiInst.showGUI(username);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (GUIConfig.debugMode) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public String getUsernameFromChat(String message) {
        try {
            String unformatted = EnumChatFormatting.getTextWithoutFormattingCodes(message);
            return unformatted.substring(unformatted.lastIndexOf("]") + 2, unformatted.lastIndexOf(":"));
        } catch (Exception e) {
            if (GUIConfig.debugMode) {
                e.printStackTrace();
            }
            return null;
        }
    }


    @SubscribeEvent
    @SuppressWarnings({"ConstantConditions", "MismatchedStringCase"})
    public void onWorldLoad(WorldEvent.Load event) {
        try {
            if (mc.getCurrentServerData().serverIP.contains("hypixel")) {
                if (GUIConfig.debugMode) {
                    LOGGER.info("on Hypixel!");
                }
                locraw = true;
                onHypixel = true;
                LocrawUtil.lobby = false;
            } else {
                onHypixel = false;
                LocrawUtil.lobby = false;
                locraw = false;
            }
        } catch (Exception e) {
            // if(GUIConfig.debugMode) {e.printStackTrace();}
        }
        if (updateCheck && GUIConfig.sendUp && event.world.isRemote) {
            new TickDelay(this::sendUpdateMessage, 20);
            updateCheck = false;
        }
        if (Reference.VERSION.contains("beta") && betaFlag && event.world.isRemote) {
            try {
                new TickDelay(() -> sendMessages("",
                        "Beta build has been detected (ver. " + Reference.VERSION + ")",
                        "Note that some features might be unstable! Use at your own risk!"), 20);
                betaFlag = false;
            } catch (Exception e) {
                betaFlag = true;
                //if (GUIConfig.debugMode) { e.printStackTrace(); }
                LOGGER.error("skipping beta message, bad world return!");
            }
        }
        if (corrupt) {
            try {
                new TickDelay(() -> sendMessages("",
                        "An error occurred while trying to read your config file. You will have to reset it.",
                        "If you just reset your configuration file, ignore this message."), 20);
                corrupt = false;
            } catch (Exception e) {
                //if (GUIConfig.debugMode) { e.printStackTrace(); }
                LOGGER.error("skipping corrupt message, bad world return!");
            }
        }
        if (AuthChecker.mismatch && GUIConfig.securityLevel == 2) {
            try {
                new TickDelay(() -> sendMessages("The hash for the mod is incorrect. Check the logs for more info.",
                        "WARNING: This could mean your data is exposed to hackers! Make sure you got the mod from the OFFICIAL mirror, and try again.",
                        Reference.URL), 20);
                AuthChecker.mismatch = false;
            } catch (Exception e) {
                //if (GUIConfig.debugMode) { e.printStackTrace();}
                LOGGER.error("skipping hash mismatch message, bad world return!");
            }
        }
    }

    @SuppressWarnings({"ConstantConditions", "MismatchedStringCase"})
    public static void sendMessages(String... messages) {
        try {
            for (String message : messages) {
                mc.thePlayer.addChatMessage(new ChatComponentText(Reference.COLOR + "[" + Reference.NAME + "] " + message));
            }
        } catch (Exception e) {
            LOGGER.error("Didn't send message: " + e.getMessage());
            //if (GUIConfig.debugMode) { e.printStackTrace(); }
            if (Reference.VERSION.contains("beta")) {
                betaFlag = true;
            }
        }
    }

    private void sendUpdateMessage() {
        try {
            IChatComponent comp = new ChatComponentText("Click here to update it!");
            ChatStyle style = new ChatStyle().setChatClickEvent(new ClickEvent(Action.OPEN_URL, Reference.URL));
            style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ChatComponentText(Reference.COLOR + Reference.URL)));
            style.setColor(Reference.COLOR);
            style.setUnderlined(true);
            comp.setChatStyle(style);
            mc.thePlayer.playSound("minecraft:random.successful_hit", 1.0F, 1.0F);
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    Reference.COLOR + "--------------------------------------"));
            mc.thePlayer.addChatMessage(new ChatComponentText(Reference.COLOR
                    + ("A newer version of " + Reference.NAME + " is available! (" + UpdateChecker.latestVersion + ")")));
            mc.thePlayer.addChatMessage(comp);
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    Reference.COLOR + "--------------------------------------"));
        } catch (NullPointerException e) {
            //if (GUIConfig.debugMode) { e.printStackTrace(); }
            updateCheck = true;
            LOGGER.error("skipping update message, bad world return!");
        }
    }
}
