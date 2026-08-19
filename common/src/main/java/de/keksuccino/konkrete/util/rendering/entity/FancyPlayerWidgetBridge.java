package de.keksuccino.konkrete.util.rendering.entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;

/**
 * Resolves and invokes Fancy Entity Renderer's player widget without placing its classes in Konkrete's linkage graph.
 * Every operation is resolved up front so availability cannot succeed with a partially compatible API.
 */
final class FancyPlayerWidgetBridge {

    private static final String WIDGET_CLASS = "it.crystalnest.fancy_entity_renderer.api.entity.player.FancyPlayerWidget";
    private static final String ROTATION_CLASS = "it.crystalnest.fancy_entity_renderer.api.Rotation";
    private static final String RENDER_MODE_CLASS = "it.crystalnest.fancy_entity_renderer.api.entity.RenderMode";

    private final Api api;
    private final Object delegate;

    private FancyPlayerWidgetBridge(Api api, Object delegate) {
        this.api = api;
        this.delegate = delegate;
    }

    static boolean isAvailable() {
        return ResolutionHolder.RESOLUTION.api != null;
    }

    static String getUnavailableReason() {
        Resolution resolution = ResolutionHolder.RESOLUTION;
        return resolution.api == null ? "Fancy Entity Renderer player widget API is incompatible: " + describeFailure(resolution.failure) : null;
    }

    static FancyPlayerWidgetBridge create(int x, int y, int width, int height) {
        Api api = requireApi();
        return new FancyPlayerWidgetBridge(api, ReflectiveAccess.construct(api.constructor, x, y, width, height));
    }

    void invoke(Operation operation, Object... arguments) {
        ReflectiveAccess.invoke(this.api.methods.get(operation), this.delegate, arguments);
    }

    boolean invokeBoolean(Operation operation) {
        return (Boolean) ReflectiveAccess.invoke(this.api.methods.get(operation), this.delegate);
    }

    Object createRotation(EntityRotation rotation) {
        return ReflectiveAccess.construct(this.api.rotationConstructor, rotation.x(), rotation.y(), rotation.z());
    }

    Object resolveRenderMode(PlayerRenderMode renderMode) {
        return this.api.renderModes.get(renderMode);
    }

    private static Api requireApi() {
        Resolution resolution = ResolutionHolder.RESOLUTION;
        if (resolution.api == null) throw new IllegalStateException("Fancy Entity Renderer player widget API is incompatible: " + describeFailure(resolution.failure), resolution.failure);
        return resolution.api;
    }

    private static Resolution resolveApi() {
        try {
            ClassLoader classLoader = FancyPlayerWidgetBridge.class.getClassLoader();
            Class<?> widgetClass = ReflectiveAccess.loadClass(WIDGET_CLASS, classLoader);
            Class<?> rotationClass = ReflectiveAccess.loadClass(ROTATION_CLASS, classLoader);
            Class<?> renderModeClass = ReflectiveAccess.loadClass(RENDER_MODE_CLASS, classLoader);
            if (!renderModeClass.isEnum()) throw new IllegalStateException(RENDER_MODE_CLASS + " is not an enum");
            Constructor<?> constructor = ReflectiveAccess.requireConstructor(widgetClass, int.class, int.class, int.class, int.class);
            Constructor<?> rotationConstructor = ReflectiveAccess.requireConstructor(rotationClass, float.class, float.class, float.class);
            Map<Operation, Method> methods = new EnumMap<>(Operation.class);
            for (Operation operation : Operation.values()) methods.put(operation, ReflectiveAccess.requireMethod(widgetClass, operation.methodName, resolveTypes(classLoader, operation.parameterTypeNames)));
            if (methods.get(Operation.IS_COPYING_PLAYER).getReturnType() != boolean.class) throw new IllegalStateException(WIDGET_CLASS + ".isCopyingPlayer() does not return boolean");
            Map<PlayerRenderMode, Object> renderModes = new EnumMap<>(PlayerRenderMode.class);
            for (PlayerRenderMode renderMode : PlayerRenderMode.values()) renderModes.put(renderMode, enumValue(renderModeClass, renderMode.name()));
            return new Resolution(new Api(constructor, rotationConstructor, Map.copyOf(renderModes), Map.copyOf(methods)), null);
        } catch (ReflectiveOperationException | LinkageError | SecurityException ex) {
            return new Resolution(null, ex);
        } catch (RuntimeException ex) {
            return new Resolution(null, ex);
        }
    }

    private static Class<?>[] resolveTypes(ClassLoader classLoader, String[] typeNames) throws ClassNotFoundException {
        Class<?>[] types = new Class<?>[typeNames.length];
        for (int i = 0; i < typeNames.length; i++) types[i] = resolveType(classLoader, typeNames[i]);
        return types;
    }

    private static Class<?> resolveType(ClassLoader classLoader, String typeName) throws ClassNotFoundException {
        return switch (typeName) {
            case "boolean" -> boolean.class;
            case "float" -> float.class;
            case "int" -> int.class;
            default -> ReflectiveAccess.loadClass(typeName, classLoader);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumValue(Class<?> enumClass, String constantName) {
        return Enum.valueOf((Class<? extends Enum>) enumClass, constantName);
    }

    private static String describeFailure(Throwable failure) {
        if (failure == null) return "unknown linkage failure";
        String message = failure.getMessage();
        return failure.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private static final class ResolutionHolder {

        private static final Resolution RESOLUTION = resolveApi();

    }

    private record Api(Constructor<?> constructor, Constructor<?> rotationConstructor, Map<PlayerRenderMode, Object> renderModes, Map<Operation, Method> methods) {
    }

    private record Resolution(Api api, Throwable failure) {
    }

    enum Operation {
        EXTRACT_RENDER_STATE("extractRenderState", "net.minecraft.client.gui.GuiGraphicsExtractor", "int", "int", "float"),
        UPDATE_NARRATION("updateNarration", "net.minecraft.client.gui.narration.NarrationElementOutput"),
        SET_X("setX", "int"),
        SET_Y("setY", "int"),
        SET_WIDTH("setWidth", "int"),
        SET_HEIGHT("setHeight", "int"),
        SET_SIZE("setSize", "int", "int"),
        SET_BODY_FOLLOWS_MOUSE("setBodyFollowsMouse", "boolean"),
        SET_HEAD_FOLLOWS_MOUSE("setHeadFollowsMouse", "boolean"),
        SET_HEAD_ROTATION("setHeadRotation", ROTATION_CLASS),
        SET_HEAD_ROTATION_DEGREES("setHeadRotation", "float", "float", "float"),
        SET_BODY_ROTATION("setBodyRotation", ROTATION_CLASS),
        SET_BODY_ROTATION_DEGREES("setBodyRotation", "float", "float", "float"),
        SET_LEFT_ARM_ROTATION("setLeftArmRotation", ROTATION_CLASS),
        SET_LEFT_ARM_ROTATION_DEGREES("setLeftArmRotation", "float", "float", "float"),
        SET_RIGHT_ARM_ROTATION("setRightArmRotation", ROTATION_CLASS),
        SET_RIGHT_ARM_ROTATION_DEGREES("setRightArmRotation", "float", "float", "float"),
        SET_LEFT_LEG_ROTATION("setLeftLegRotation", ROTATION_CLASS),
        SET_LEFT_LEG_ROTATION_DEGREES("setLeftLegRotation", "float", "float", "float"),
        SET_RIGHT_LEG_ROTATION("setRightLegRotation", ROTATION_CLASS),
        SET_RIGHT_LEG_ROTATION_DEGREES("setRightLegRotation", "float", "float", "float"),
        SET_SLIM("setSlim", "boolean"),
        SET_SKIN("setSkin", "net.minecraft.world.entity.player.PlayerSkin"),
        COPY_LOCAL_PLAYER("copyLocalPlayer"),
        SET_NAME("setName", "java.lang.String"),
        SET_PIN_NAME("setPinName", "boolean"),
        SET_SHOW_NAME("setShowName", "boolean"),
        SET_SHOW_CAPE("setShowCape", "boolean"),
        SET_SHOW_LEFT_ARM("setShowLeftArm", "boolean"),
        SET_SHOW_LEFT_SLEEVE("setShowLeftSleeve", "boolean"),
        SET_SHOW_RIGHT_ARM("setShowRightArm", "boolean"),
        SET_SHOW_RIGHT_SLEEVE("setShowRightSleeve", "boolean"),
        SET_SHOW_LEFT_LEG("setShowLeftLeg", "boolean"),
        SET_SHOW_LEFT_PANTS("setShowLeftPants", "boolean"),
        SET_SHOW_RIGHT_LEG("setShowRightLeg", "boolean"),
        SET_SHOW_RIGHT_PANTS("setShowRightPants", "boolean"),
        SET_SHOW_HEAD("setShowHead", "boolean"),
        SET_SHOW_HAT("setShowHat", "boolean"),
        SET_SHOW_BODY("setShowBody", "boolean"),
        SET_SHOW_JACKET("setShowJacket", "boolean"),
        SET_UPSIDE_DOWN("setUpsideDown", "boolean"),
        SET_POSE("setPose", "net.minecraft.world.entity.Pose"),
        SET_RENDER_MODE("setRenderMode", RENDER_MODE_CLASS),
        SET_GLOWING("setGlowing", "int"),
        SET_MOVING("setMoving", "boolean"),
        SET_ON_FIRE("setOnFire", "boolean"),
        SET_ON_FIRE_TYPE("setOnFire", "boolean", "net.minecraft.resources.Identifier"),
        SET_BABY("setBaby", "boolean"),
        SET_PARROTS("setParrots", "net.minecraft.world.entity.animal.parrot.Parrot$Variant", "net.minecraft.world.entity.animal.parrot.Parrot$Variant"),
        SET_RIGHT_HAND_ITEM("setRightHandItem", "net.minecraft.world.item.Item"),
        SET_RIGHT_HAND_ITEM_STACK("setRightHandItem", "net.minecraft.world.item.ItemStack"),
        SET_LEFT_HAND_ITEM("setLeftHandItem", "net.minecraft.world.item.Item"),
        SET_LEFT_HAND_ITEM_STACK("setLeftHandItem", "net.minecraft.world.item.ItemStack"),
        SET_HEAD_WEARABLE_STRING("setHeadWearable", "java.lang.String", "net.minecraft.core.HolderLookup$Provider"),
        SET_CHEST_WEARABLE_STRING("setChestWearable", "java.lang.String", "net.minecraft.core.HolderLookup$Provider"),
        SET_LEGS_WEARABLE_STRING("setLegsWearable", "java.lang.String", "net.minecraft.core.HolderLookup$Provider"),
        SET_FEET_WEARABLE_STRING("setFeetWearable", "java.lang.String", "net.minecraft.core.HolderLookup$Provider"),
        SET_HEAD_WEARABLE("setHeadWearable", "net.minecraft.world.item.Item"),
        SET_CHEST_WEARABLE("setChestWearable", "net.minecraft.world.item.Item"),
        SET_LEGS_WEARABLE("setLegsWearable", "net.minecraft.world.item.Item"),
        SET_FEET_WEARABLE("setFeetWearable", "net.minecraft.world.item.Item"),
        SET_HEAD_WEARABLE_STACK("setHeadWearable", "net.minecraft.world.item.ItemStack"),
        SET_CHEST_WEARABLE_STACK("setChestWearable", "net.minecraft.world.item.ItemStack"),
        SET_LEGS_WEARABLE_STACK("setLegsWearable", "net.minecraft.world.item.ItemStack"),
        SET_FEET_WEARABLE_STACK("setFeetWearable", "net.minecraft.world.item.ItemStack"),
        COPY_PLAYER_NAME("copyPlayer", "java.lang.String"),
        COPY_PLAYER_ID("copyPlayer", "java.util.UUID"),
        UNCOPY_PLAYER("uncopyPlayer"),
        IS_COPYING_PLAYER("isCopyingPlayer");

        private final String methodName;
        private final String[] parameterTypeNames;

        Operation(String methodName, String... parameterTypeNames) {
            this.methodName = methodName;
            this.parameterTypeNames = parameterTypeNames;
        }
    }

}
