package br.cropmanager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

/**
 * OP command (/cmshowcase) available only when the runtimeTest addon is loaded.
 */
public final class ShowcaseCommand extends CommandBase {
    private static final List<String> SECTORS = Arrays.asList(
            "reception", "hub", "farm", "multiblocks", "chemistry", "greenhouse", "hydroponics", "irrigation"
    );

    @Override
    public String getName() {
        return "cmshowcase";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/cmshowcase [pt_br|en_us] | /cmshowcase tp <" + String.join("|", SECTORS) + ">";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        WorldServer world = (WorldServer) sender.getEntityWorld();
        EntityPlayerMP player = sender instanceof EntityPlayerMP ? (EntityPlayerMP) sender : null;

        if (args.length >= 2 && "tp".equalsIgnoreCase(args[0])) {
            if (player == null) {
                throw new CommandException("Player required for /cmshowcase tp");
            }
            teleportToSector(player, ShowcaseFarmBuilder.DEFAULT_ORIGIN, args[1].toLowerCase());
            return;
        }

        String locale = args.length >= 1 ? args[0] : "pt_br";
        ShowcaseFarmBuilder.ShowcaseSummary summary = ShowcaseFarmBuilder.build(
                world, ShowcaseFarmBuilder.DEFAULT_ORIGIN, locale, player);
        ShowcaseFarmBuilder.verifyShowcaseIntegrity(world, summary);

        if (player != null) {
            teleportToSector(player, ShowcaseFarmBuilder.DEFAULT_ORIGIN, "reception");
        }
        sender.sendMessage(new TextComponentString("[CropManager] Showcase Farm built & verified (" + locale + ")."));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "pt_br", "en_us", "tp");
        }
        if (args.length == 2 && "tp".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, SECTORS);
        }
        return Collections.emptyList();
    }

    private static void teleportToSector(EntityPlayerMP player, BlockPos origin, String sector) throws CommandException {
        double x, y = origin.getY() + 1.0, z;
        float yaw, pitch = 12.0f;
        switch (sector) {
            case "reception":
                x = origin.getX() + 0.5;
                z = origin.getZ() + 28.5;
                yaw = 180.0f;
                break;
            case "hub":
                x = origin.getX() + 0.5;
                z = origin.getZ() + 14.5;
                yaw = 180.0f;
                break;
            case "farm":
                x = origin.getX() + 0.5;
                z = origin.getZ() - 4.5;
                yaw = 180.0f;
                break;
            case "multiblocks":
                x = origin.getX() + 15.5;
                z = origin.getZ() - 1.5;
                yaw = -90.0f;
                break;
            case "chemistry":
                x = origin.getX() - 15.5;
                z = origin.getZ() - 1.5;
                yaw = 90.0f;
                break;
            case "greenhouse":
                x = origin.getX() - 16.5;
                z = origin.getZ() + 19.5;
                yaw = 90.0f;
                break;
            case "hydroponics":
                x = origin.getX() + 18.5;
                z = origin.getZ() + 22.5;
                yaw = -140.0f;
                break;
            case "irrigation":
                x = origin.getX() + 25.5;
                z = origin.getZ() - 14.5;
                yaw = 180.0f;
                break;
            default:
                throw new CommandException("Unknown sector: " + sector);
        }
        player.connection.setPlayerLocation(x, y, z, yaw, pitch);
    }
}
