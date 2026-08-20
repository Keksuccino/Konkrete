package de.keksuccino.konkrete.networking.packets.placeholders.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.keksuccino.konkrete.networking.PacketHandler;
import de.keksuccino.konkrete.networking.PacketSendResult;
import de.keksuccino.konkrete.util.nbt.NbtNumericValueFormatter;
import de.keksuccino.konkrete.util.rendering.text.ComponentParser;
import net.minecraft.advancements.predicates.NbtPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.CommandStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** Resolves bounded vanilla data-command-style NBT queries on the server main thread. */
final class ServerSideServerNbtDataRequestPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_SOURCE_TYPE_LENGTH = 16;
    private static final int MAX_ENTITY_SELECTOR_LENGTH = 1024;
    private static final int MAX_BLOCK_POSITION_LENGTH = 256;
    private static final int MAX_STORAGE_ID_LENGTH = 256;
    private static final int MAX_NBT_PATH_LENGTH = 2048;
    private static final int MAX_RETURN_TYPE_LENGTH = 16;

    private ServerSideServerNbtDataRequestPacketLogic() {
    }

    static boolean handle(@NotNull ServerPlayer sender, @NotNull ServerNbtDataRequestPacket packet) {
        if (packet.requestId() <= 0) return false;
        ServerNbtQuery query = packet.query();
        String result = null;
        if (isBounded(query)) {
            try {
                CommandContextData context = buildContext(sender, query);
                Tag tag = context == null ? null : resolveTag(context, query.nbtPath());
                if (tag != null) result = convertResult(tag, query, context);
            } catch (CommandSyntaxException | RuntimeException exception) {
                LOGGER.debug("[KONKRETE] Server-side NBT placeholder query could not be resolved.", exception);
            }
        }
        return PacketHandler.sendToClient(sender, new ServerNbtDataResponsePacket(packet.requestId(), result)) == PacketSendResult.SENT;
    }

    static boolean isBounded(@NotNull ServerNbtQuery query) {
        return isWithin(query.sourceType(), MAX_SOURCE_TYPE_LENGTH)
                && isWithin(query.entitySelector(), MAX_ENTITY_SELECTOR_LENGTH)
                && isWithin(query.blockPosition(), MAX_BLOCK_POSITION_LENGTH)
                && isWithin(query.storageId(), MAX_STORAGE_ID_LENGTH)
                && isWithin(query.nbtPath(), MAX_NBT_PATH_LENGTH)
                && isWithin(query.returnType(), MAX_RETURN_TYPE_LENGTH)
                && (query.scale() == null || Double.isFinite(query.scale()));
    }

    private static boolean isWithin(@Nullable String value, int maximumLength) {
        return value == null || value.length() <= maximumLength;
    }

    private static @Nullable CommandContextData buildContext(@NotNull ServerPlayer sender, @NotNull ServerNbtQuery query) throws CommandSyntaxException {
        if (query.sourceType() == null) return null;
        return switch (query.sourceType().toLowerCase(Locale.ROOT)) {
            case "entity" -> resolveEntity(sender, query.entitySelector());
            case "block" -> resolveBlock(sender, query.blockPosition());
            case "storage" -> resolveStorage(sender, query.storageId());
            default -> null;
        };
    }

    private static @Nullable CommandContextData resolveEntity(@NotNull ServerPlayer sender, @Nullable String selectorString) throws CommandSyntaxException {
        if (selectorString == null || selectorString.isEmpty()) return null;
        EntitySelector selector = new EntitySelectorParser(new StringReader(selectorString), true).parse();
        CommandSourceStack source = sender.createCommandSourceStack();
        CompoundTag tag = NbtPredicate.getEntityTagToCompare(selector.findSingleEntity(source));
        return new CommandContextData(tag, source);
    }

    private static @Nullable CommandContextData resolveBlock(@NotNull ServerPlayer sender, @Nullable String positionString) throws CommandSyntaxException {
        if (positionString == null || positionString.isEmpty()) return null;
        ServerLevel level = sender.level();
        CommandSourceStack source = sender.createCommandSourceStack();
        Coordinates coordinates = BlockPosArgument.blockPos().parse(new StringReader(positionString));
        BlockPos position = coordinates.getBlockPos(source);
        if (!level.hasChunkAt(position) || !level.isInWorldBounds(position)) return null;
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (blockEntity == null) return null;
        return new CommandContextData(blockEntity.saveWithFullMetadata(level.registryAccess()), source);
    }

    private static @Nullable CommandContextData resolveStorage(@NotNull ServerPlayer sender, @Nullable String storageId) {
        if (storageId == null || storageId.isEmpty()) return null;
        Identifier identifier = Identifier.tryParse(storageId);
        if (identifier == null) return null;
        CommandSourceStack source = sender.createCommandSourceStack();
        CommandStorage storage = sender.level().getServer().getCommandStorage();
        return new CommandContextData(storage.get(identifier), source);
    }

    private static @Nullable Tag resolveTag(@NotNull CommandContextData context, @Nullable String nbtPath) throws CommandSyntaxException {
        if (context.baseTag() == null) return null;
        if (nbtPath == null || nbtPath.isEmpty()) return context.baseTag();
        List<Tag> tags = NbtPathArgument.nbtPath().parse(new StringReader(nbtPath)).get(context.baseTag());
        return tags.isEmpty() ? null : tags.getFirst();
    }

    private static @NotNull String convertResult(@NotNull Tag tag, @NotNull ServerNbtQuery query, @NotNull CommandContextData context) throws CommandSyntaxException {
        String returnType = query.returnType() == null ? "value" : query.returnType().toLowerCase(Locale.ROOT);
        double scale = query.scale() == null ? 1.0D : query.scale();
        return switch (returnType) {
            case "string" -> tag.asString().orElse("");
            case "snbt" -> tag.toString();
            case "json" -> {
                if (tag instanceof CompoundTag) {
                    Component component = NbtUtils.toPrettyComponent(tag);
                    yield ComponentParser.toJson(component, context.source().registryAccess());
                }
                yield tag.toString();
            }
            default -> tag instanceof NumericTag numericTag ? NbtNumericValueFormatter.format(numericTag, scale) : tag.asString().orElse("");
        };
    }

    private record CommandContextData(@Nullable Tag baseTag, @NotNull CommandSourceStack source) {

    }

}
