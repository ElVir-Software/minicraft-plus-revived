package minicraft.screen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import minicraft.core.Game;
import minicraft.core.io.ControllerHandler;
import minicraft.core.io.InputHandler;
import minicraft.core.io.Localization;
import minicraft.core.io.Settings;
import minicraft.gfx.Color;
import minicraft.saveload.Save;
import minicraft.screen.entry.BlankEntry;
import minicraft.screen.entry.ListEntry;
import minicraft.screen.entry.SelectEntry;
import minicraft.screen.entry.StringEntry;

public class OptionsWorldDisplay extends Display {
	private boolean confirmOff = false;

	public OptionsWorldDisplay() {
		super(true);

		List<ListEntry> entries = getEntries();

		if ((boolean) Settings.get("tutorials")) {
			entries.add(new BlankEntry());
			entries.add(new SelectEntry("minicraft.displays.options_world.turn_off_tutorials", () -> {
				confirmOff = true;
				selection = 1;
				menus[selection].shouldRender = true;
			}));
		}

		menus = new Menu[] {
			new Menu.Builder(false, 6, RelPos.LEFT, entries)
					.setTitle("minicraft.displays.options_world")
					.createMenu(),
			new Menu.Builder(true, 4, RelPos.CENTER)
				.setShouldRender(false)
				.setSelectable(false)
				.setEntries(StringEntry.useLines(Color.RED, "minicraft.displays.options_world.off_tutorials_confirm_popup",
					"minicraft.display.popup.enter_confirm", "minicraft.display.popup.escape_cancel"))
				.setTitle("minicraft.display.popup.title_confirm")
				.createMenu()
		};
	}

	@Override
	public void tick(InputHandler input, ControllerHandler controlInput) {
		if (confirmOff) {
			if (input.isClicked("exit", controlInput)) {
				confirmOff = false;
				menus[1].shouldRender = false;
				selection = 0;
			} else if (input.isClicked("select", controlInput)) {
				confirmOff = false;
				QuestsDisplay.tutorialOff();

				menus[1].shouldRender = false;
				menus[0].setEntries(getEntries());
				menus[0].setSelection(0);
				selection = 0;
			}

			return;
		}

		super.tick(input, controlInput); // ticks menu
	}

	private List<ListEntry> getEntries() {
		return new ArrayList<>(Arrays.asList(Settings.getEntry("diff"),
			Settings.getEntry("fps"),
			Settings.getEntry("sound"),
			Settings.getEntry("autosave"),
			Settings.getEntry("showquests"),
			new SelectEntry("minicraft.display.options_display.change_key_bindings", () -> Game.setDisplay(new KeyInputDisplay())),
			Settings.getEntry("language")
		));
	}

	@Override
	public void onExit() {
		Localization.changeLanguage((String)Settings.get("language"));
		new Save();
		Game.MAX_FPS = (int)Settings.get("fps");
	}
}
