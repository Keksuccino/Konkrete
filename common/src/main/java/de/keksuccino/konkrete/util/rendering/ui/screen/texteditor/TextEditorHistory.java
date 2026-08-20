package de.keksuccino.konkrete.util.rendering.ui.screen.texteditor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/** Stores undo and redo snapshots for a text editor. */
public class TextEditorHistory {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Editor whose document state is captured and restored. */
    protected TextEditorWindowBody parent;
    /** Document snapshots ordered from oldest to newest. */
    protected List<Snapshot> snapshots = new ArrayList<>();
    /** Index of the next snapshot, allowing {@code size()} to represent the live document. */
    protected int index = 0;

    /** Creates an undo/redo history owned by the supplied text editor. */
    protected TextEditorHistory(@NotNull TextEditorWindowBody parent) {
        this.parent = parent;
    }

    /** Captures the document, focus, cursor, and scroll state unless it duplicates the latest snapshot. */
    public void saveSnapshot() {
        try {
            if (this.index > 0) {
                if (this.snapshots.get(this.index-1).text.equals(this.parent.getText())) {
                    return; //don't save snapshot if duplicate of index-1 (to not create two identical snaps in a row)
                }
            }
            if (this.index < this.snapshots.size()) {
                if (this.index == 0) {
                    this.snapshots.clear();
                } else {
                    this.snapshots = this.snapshots.subList(0, this.index);
                }
            }
            TextEditorLine focusedLine = this.parent.getFocusedLine();
            int cursorPos = (focusedLine != null) ? focusedLine.getCursorPosition() : 0;
            this.snapshots.add(new Snapshot(this.parent.getText(), this.parent.getFocusedLineIndex(), cursorPos, this.parent.verticalScrollBar.getScroll(), this.parent.horizontalScrollBar.getScroll()));
            this.index = this.snapshots.size();
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to capture a text-editor history snapshot.", ex);
        }
    }

    /** Restores the preceding snapshot, first preserving an unsaved live document when necessary. */
    public void stepBack() {
        try {
            if (this.snapshots.isEmpty()) return;
            if (this.index > 0) {
                if (this.index == this.snapshots.size()) {
                    if (!this.snapshots.get(this.index-1).text.equals(this.parent.getText())) {
                        //save snapshot before going back if index at end of snapshots and no snapshot with the current content already exists
                        this.saveSnapshot();
                        this.index--;
                    } else if (this.index > 1) {
                        this.index--;
                    }
                }
                this.index--;
                this.restoreFrom(this.snapshots.get(this.index));
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to step backward through text-editor history.", ex);
        }
    }

    /** Restores the next snapshot without advancing beyond the live-document position. */
    public void stepForward() {
        try {
            if (this.snapshots.isEmpty()) return;
            this.index++;
            if (this.index >= this.snapshots.size()) this.index = this.snapshots.size()-1;
            this.restoreFrom(this.snapshots.get(this.index));
            if (this.index == this.snapshots.size()-1) this.index = this.snapshots.size();
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to step forward through text-editor history.", ex);
        }
    }

    /** Restores document content, focused line, cursor, and both scroll positions. */
    protected void restoreFrom(@NotNull Snapshot snap) {
        this.parent.setText(snap.text);
        if (snap.focusedLineIndex != -1) {
            this.parent.setFocusedLine(snap.focusedLineIndex);
            TextEditorLine focused = this.parent.getFocusedLine();
            if (focused != null) {
                focused.setCursorPosition(snap.cursorPos);
                focused.setHighlightPos(snap.cursorPos);
            }
        }
        this.parent.verticalScrollBar.setScroll(snap.verticalScroll, false);
        this.parent.horizontalScrollBar.setScroll(snap.horizontalScroll, false);
    }

    /** Captures snapshot at one point in its lifecycle. */
    public record Snapshot(@NotNull String text, int focusedLineIndex, int cursorPos, float verticalScroll, float horizontalScroll) {

    }

}
