package com.connectedsigns.client;

import com.connectedsigns.SignGroup;
import com.connectedsigns.SignGroupCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MultiSignEditScreen extends Screen {
    private final List<BlockPos> signPositions;
    private final List<String[]> signLines;

    private final String[] rows = new String[4];
    private int cursorRow = 0;
    private int cursorCol = 0;
    private boolean cursorVisible = true;
    private int cursorTimer = 0;

    private static final int LINE_HEIGHT = 10;
    private static final int LINES_PER_SIGN = 4;
    private static final int MAX_CHARS_PER_LINE = 15;
    private static final int SIGN_WIDTH = 110;
    private static final int SIGN_HEIGHT = 60;
    private static final int TEXT_COLOR = 0xFF000000;

    private final BlockPos clickedPos;

    private Identifier signTexture;

    public MultiSignEditScreen(List<BlockPos> positions, World world, BlockPos clickedPos) {
        super(Text.literal("Edit Signs"));
        this.signPositions = positions;
        this.clickedPos = clickedPos;
        this.signLines = new ArrayList<>();

        for (BlockPos pos : positions) {
            String[] lines = new String[LINES_PER_SIGN];
            if (world.getBlockEntity(pos) instanceof SignBlockEntity sign) {
                SignText front = sign.getFrontText();
                for (int i = 0; i < LINES_PER_SIGN; i++) {
                    lines[i] = front.getMessage(i, false).getString();
                }
            } else {
                for (int i = 0; i < LINES_PER_SIGN; i++) lines[i] = "";
            }
            signLines.add(lines);
        }

        signTexture = getSignTexture(world, positions.get(0));

        for (int row = 0; row < LINES_PER_SIGN; row ++) {
            StringBuilder sb = new StringBuilder();
            for (String[] sl : signLines) {
                sb.append(sl[row]);
            }
            rows[row] = sb.toString();
        }

        int maxChars = MAX_CHARS_PER_LINE * positions.size();
        for (int row = 0; row < LINES_PER_SIGN; row++) {
            if (rows[row].length() > maxChars) {
                rows[row] = rows[row].substring(0, maxChars);
            }
        }
        cursorCol = rows[0].length();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, 0x90000000, 0x90000000);
    }

    private Identifier getSignTexture(World world, BlockPos pos) {
        String blockId = Registries.BLOCK.getId(world.getBlockState(pos).getBlock()).getPath();
        String woodType = blockId.replace("_wall_sign", "");
        String namespace = Registries.BLOCK.getId(world.getBlockState(pos).getBlock()).getNamespace();

        Identifier texture = Identifier.of(namespace, "textures/entity/signs/" + woodType + ".png");
        Identifier fallback = Identifier.of("minecraft", "textures/entity/signs/oak.png");

        if (MinecraftClient.getInstance().getResourceManager().getResource(texture).isPresent()) {
            return texture;
        }
        return fallback;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int buttonY = height / 2 + 70;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
                .dimensions(centerX + 5, buttonY, 100, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal(SignGroupCache.isIndividual(clickedPos) ? "Individual: YES" : "Individual: NO"), btn -> {
            if (SignGroupCache.isIndividual(clickedPos)) {
                SignGroupCache.unmarkIndividual(clickedPos);
                BigSignClientNetwork.sendMarkIndividual(clickedPos, false);
                btn.setMessage(Text.literal("Individual: NO"));
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.world != null) {
                    List<BlockPos> group = SignGroup.findConnectedSigns(client.world, clickedPos);
                    client.setScreen(new MultiSignEditScreen(group, client.world, clickedPos));
                }
            } else {
                SignGroupCache.markIndividual(clickedPos);
                BigSignClientNetwork.sendMarkIndividual(clickedPos, true);
                btn.setMessage(Text.literal("Individual: YES"));
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.world != null) {
                    client.setScreen(new MultiSignEditScreen(List.of(clickedPos), client.world, clickedPos));
                }
            }
        }).dimensions(centerX - 105, buttonY, 100, 20).build());
        setFocused(null);
    }

    private void renderSignModels(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        int numSigns = signPositions.size();
        int totalWidth = numSigns * SIGN_WIDTH;
        int startX = (width - totalWidth) / 2;
        int centerY = height / 2;

        for (int s = 0; s < numSigns; s++) {
            BlockPos pos = signPositions.get(s);
            BlockState state = client.world.getBlockState(pos);
            if (!(client.world.getBlockEntity(pos) instanceof SignBlockEntity sign)) continue;

            int signCenterX = startX + s * SIGN_WIDTH + SIGN_WIDTH / 2;

            Direction facing = state.get(WallSignBlock.FACING);
            float yaw = switch (facing) {
                case NORTH -> 180f;
                case SOUTH -> 0f;
                case EAST -> 90f;
                case WEST -> 270f;
                default -> 0f;
                // i hate switch statements
            };

            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(signCenterX, centerY, 50);
            matrices.scale(80, -80, 80);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));

            VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

            BlockEntityRenderer<SignBlockEntity> renderer = client.getBlockEntityRenderDispatcher().get(sign);
            if (renderer != null) {
                renderer.render(sign, 0f, matrices, immediate, 0xF000F0, OverlayTexture.DEFAULT_UV);
            }

            immediate.draw();
            matrices.pop();
        }
    }

    private void drawCursor(DrawContext context, int textX, int lineY, String rowText) {
        String beforeCursor = rowText.substring(0, Math.min(cursorCol, rowText.length()));
        int cursorX = textX + textRenderer.getWidth(beforeCursor);
        context.fill(cursorX, lineY - 1, cursorX + 1, lineY + LINE_HEIGHT - 1, TEXT_COLOR);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta); // background + widgets first

        int numSigns = signPositions.size();
        int totalWidth = numSigns * SIGN_WIDTH;
        int startX = (width - totalWidth) / 2;
        int startY = height / 2 - SIGN_HEIGHT / 2;

        for (int s = 0; s < numSigns; s++) {
            int sx = startX + s * SIGN_WIDTH;

            // Stretch the sign face region (U=2,V=2,W=24,H=12 in a 64x32 entity texture to fill SIGN_WIDTH * SIGN_HEIGHT by scaling the matrix stack.
            MatrixStack ms = context.getMatrices();
            ms.push();
            ms.translate(sx, startY, 0);
            ms.scale((float) SIGN_WIDTH / 24f, (float) SIGN_HEIGHT / 12f, 1f);
            context.drawTexture(signTexture, 0, 0, 2, 2, 24, 12, 64, 32);
            ms.pop();

        }

        int maxCharsPerRow = maxCharsForGroup(numSigns);
        for (int row = 0; row < LINES_PER_SIGN; row++) {
            // int lineY = startY + 10 + row * LINE_HEIGHT;
            int centerX = startX + totalWidth / 2;

            for (row = 0; row < LINES_PER_SIGN; row++) {
                String rowText = rows[row];
                int lineY = startY + 8 + row * LINE_HEIGHT;
                int textX = centerX - textRenderer.getWidth(rowText) / 2;
                context.drawText(textRenderer, rowText, textX, lineY, TEXT_COLOR, false);
                if (cursorRow == row && cursorVisible) drawCursor(context, textX, lineY, rowText);
            }
        }
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Editing " + numSigns + " sign" + (numSigns > 1 ? "s" : "")), width / 2, startY - 16, 0xFFFFFF);
    }

    @Override
    public void tick() {
        cursorTimer++;
        if (cursorTimer >= 10) {
            cursorVisible = !cursorVisible;
            cursorTimer = 0;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 265 -> { // up
                if (cursorRow > 0) cursorRow--;
                cursorCol = rows[cursorRow].length();
                return true;
            }
            case 264 -> { // down
                if (cursorRow < LINES_PER_SIGN - 1) cursorRow++;
                cursorCol = rows[cursorRow].length();
                return true;
            }
            case 263 -> { // left
                if (cursorCol > 0) cursorCol--;
                return true;
            }
            case 262 -> { // right
                if (cursorCol < rows[cursorRow].length()) cursorCol++;
                return true;
            }
            case 257, 335 -> { // enter
                    if (cursorRow < LINES_PER_SIGN - 1) {
                        cursorRow++;
                        cursorCol = rows[cursorRow].length();
                    }
                return true;
            }
            case 259 -> { // backspace
                if (cursorCol > 0) {
                    String row = rows[cursorRow];
                    rows[cursorRow] = row.substring(0, cursorCol - 1)
                            + row.substring(cursorCol);
                    cursorCol--;
                }
                return true;
            }
            case 256 -> { // esc
                close();
                return true;
            }
            // I hate switch statements
            // literally the bane of my existence ive had to re-write this so many times
            // (26-05-2026) literally have to edit this stupid switch statement again -_-
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        int maxChars = maxCharsForGroup(signPositions.size());
        String row = rows[cursorRow];
        if (row.length() < maxChars) {
            rows[cursorRow] = row.substring(0, cursorCol) + chr + row.substring(cursorCol);
            cursorCol++;
        }
        return true;
    }

    private static int maxCharsForGroup(int numSigns) {
        return MAX_CHARS_PER_LINE * numSigns + (numSigns - 1) * 1;
    }

    private void clampCursor() {
        cursorCol = Math.min(cursorCol, rows[cursorRow].length());
    }

    private List<String[]> buildSignLines() {
        List<String[]> result = new ArrayList<>();
        for (int s = 0; s < signPositions.size(); s++) {
            String[] lines = new String[LINES_PER_SIGN];
            if (s == 0) {
                for (int row = 0; row < LINES_PER_SIGN; row++) lines[row] = rows[row];
            } else {
                for (int row = 0; row < LINES_PER_SIGN; row++) lines[row] = "";
            }
            result.add(lines);
        }
        return result;
    }

    @Override
    public void close() {
        List<String[]> signLines = buildSignLines();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isIntegratedServerRunning()) {
            client.getServer().execute(() -> {
                ServerWorld serverWorld = client.getServer().getOverworld();
                for (int i = 0; i < signPositions.size(); i++) {
                    BlockPos pos = signPositions.get(i);
                    String[] lines = signLines.get(i);
                    if (serverWorld.getBlockEntity(pos) instanceof SignBlockEntity sign) {
                        sign.changeText(oldText -> {
                            SignText newText = oldText;
                            for (int row = 0; row < LINES_PER_SIGN; row++) {
                                newText = newText.withMessage(row, Text.literal(lines[row]));
                            }
                            return newText;
                        }, true);
                        sign.markDirty();
                    }
                }
            });
        } else {
            BigSignClientNetwork.sendMultiSignUpdate(signPositions, signLines);
        }

        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}