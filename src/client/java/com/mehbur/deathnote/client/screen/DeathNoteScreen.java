package com.mehbur.deathnote.client.screen;

import com.mehbur.deathnote.network.WriteNamePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

/**
 * Black-and-gold name input. It only collects a name and sends it; the server decides everything else.
 */
public final class DeathNoteScreen extends Screen {
	private static final int PANEL_WIDTH = 220;
	private static final int PANEL_HEIGHT = 108;
	private static final int BUTTON_WIDTH = 85;

	private static final int COLOR_PANEL = 0xF0101010;
	private static final int COLOR_BORDER = 0xFFD4AF37;
	private static final int COLOR_GOLD = 0xFFFFAA00;

	private final InteractionHand hand;
	private EditBox nameField;
	private Button writeButton;

	public DeathNoteScreen(InteractionHand hand) {
		super(Component.translatable("deathnote.screen.title"));
		this.hand = hand;
	}

	private int panelLeft() {
		return (this.width - PANEL_WIDTH) / 2;
	}

	private int panelTop() {
		return (this.height - PANEL_HEIGHT) / 2;
	}

	@Override
	protected void init() {
		int left = panelLeft();
		int top = panelTop();

		this.nameField = new EditBox(this.font, left + 20, top + 38, PANEL_WIDTH - 40, 20,
				Component.translatable("deathnote.screen.prompt"));
		this.nameField.setMaxLength(WriteNamePayload.MAX_NAME_LENGTH);
		this.nameField.setTextColor(COLOR_GOLD);
		this.nameField.setHint(Component.translatable("deathnote.screen.prompt").withStyle(ChatFormatting.DARK_GRAY));
		this.addRenderableWidget(this.nameField);

		this.writeButton = this.addRenderableWidget(Button
				.builder(Component.translatable("deathnote.screen.write"), button -> submit())
				.bounds(left + 20, top + 72, BUTTON_WIDTH, 20)
				.build());
		this.addRenderableWidget(Button
				.builder(Component.translatable("deathnote.screen.cancel"), button -> onClose())
				.bounds(left + PANEL_WIDTH - 20 - BUTTON_WIDTH, top + 72, BUTTON_WIDTH, 20)
				.build());

		this.writeButton.active = false;
		this.nameField.setResponder(value -> this.writeButton.active = !value.isBlank());
		this.setInitialFocus(this.nameField);
	}

	private void submit() {
		String name = this.nameField.getValue().trim();
		if (name.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new WriteNamePayload(name, this.hand));
		onClose();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isConfirmation() && this.nameField.isFocused()) {
			submit();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int left = panelLeft();
		int top = panelTop();
		graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_PANEL);
		graphics.outline(left, top, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, panelTop() + 14, COLOR_GOLD);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
