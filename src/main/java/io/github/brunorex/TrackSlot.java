package io.github.brunorex;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 * One track slot: the settings card and every control it holds — the model
 * behind a single row of the track combo — issue #12.
 *
 * <p>
 * This replaces the parallel per-type arrays that used to live in
 * {@link JMkvpropedit} ({@code chbEditVideo[]}, {@code txtNameAudio[]},
 * {@code cbLangSubtitle[]}, ...): one slot object owns its own components, so
 * adding, removing and reading a track never has to shuffle twenty-three
 * arrays in lockstep. The card layout and enable/disable listeners are the
 * single copy of what was repeated for video, audio and subtitle.
 * </p>
 *
 * <p>
 * The original listeners looked the target components up by the track combo's
 * selected index; a slot's listeners now act on the slot's own components.
 * Both are equivalent for anything reachable through the UI: only the selected
 * slot's card is visible, so user interaction always comes from the selected
 * slot.
 * </p>
 */
public final class TrackSlot {

    /** The checkbox and Yes/No pair of one {@link TrackFlag}. */
    static final class FlagControls {
        final JCheckBox box;
        final JRadioButton yes = new JRadioButton("Yes");
        final JRadioButton no = new JRadioButton("No");

        FlagControls(TrackFlag flag) {
            box = new JCheckBox(flag.label());
            box.setToolTipText(flag.tooltip() + " (" + flag.property() + ")");
        }
    }

    /** The settings card shown in the {@link TrackPanel} CardLayout. */
    final JPanel card = new JPanel();

    final JCheckBox chbEdit = new JCheckBox("Edit this track:");

    /** Every flag's controls, in {@link TrackFlag} order. */
    final Map<TrackFlag, FlagControls> flags = new EnumMap<>(TrackFlag.class);

    // Shortcuts to the original three flags (kept for existing callers).
    final JCheckBox chbEnable;
    final JRadioButton rbYesEnable;
    final JRadioButton rbNoEnable;
    final JCheckBox chbDefault;
    final JRadioButton rbYesDef;
    final JRadioButton rbNoDef;
    final JCheckBox chbForced;
    final JRadioButton rbYesForced;
    final JRadioButton rbNoForced;

    final JCheckBox chbName = new JCheckBox("Track name:");
    final JTextField txtName = new JTextField();

    final JCheckBox chbNumb = new JCheckBox("Numbering:");
    final JLabel lblNumbStart = new JLabel("Start");
    final JTextField txtNumbStart = new JTextField();
    final JLabel lblNumbPad = new JLabel("Padding");
    final JTextField txtNumbPad = new JTextField();
    final JLabel lblNumbExplain;

    final JCheckBox chbLang = new JCheckBox("Language:");
    final JComboBox<String> cbLang = new JComboBox<>();
    final JLabel lblLangTag = new JLabel("or BCP 47 tag:");
    final JTextField txtLangTag = new JTextField();

    final JCheckBox chbExtra = new JCheckBox("Extra parameters:");
    final JTextField txtExtra = new JTextField();

    /**
     * Builds the card for one slot of the given track type: layout, initial
     * enabled states, language combo contents and all listeners.
     *
     * @param type       the track type (supplies the numbering explain example)
     * @param mkvStrings language names/codes for the language combo
     */
    TrackSlot(TrackType type, MkvStrings mkvStrings) {
        for (TrackFlag flag : TrackFlag.values()) {
            flags.put(flag, new FlagControls(flag));
        }

        chbEnable = flags.get(TrackFlag.ENABLED).box;
        rbYesEnable = flags.get(TrackFlag.ENABLED).yes;
        rbNoEnable = flags.get(TrackFlag.ENABLED).no;
        chbDefault = flags.get(TrackFlag.DEFAULT).box;
        rbYesDef = flags.get(TrackFlag.DEFAULT).yes;
        rbNoDef = flags.get(TrackFlag.DEFAULT).no;
        chbForced = flags.get(TrackFlag.FORCED).box;
        rbYesForced = flags.get(TrackFlag.FORCED).yes;
        rbNoForced = flags.get(TrackFlag.FORCED).no;

        lblNumbExplain = new JLabel(
                "      Add {num} to the name (e.g. \"" + type.nameExample()
                        + " {num}\"); {file_name} inserts the file name.");

        buildCard(mkvStrings);
        wireListeners();
    }

    /**
     * The flags to set: ticked checkboxes mapped to their Yes/No choice.
     */
    Map<TrackFlag, Boolean> selectedFlags() {
        Map<TrackFlag, Boolean> selected = new EnumMap<>(TrackFlag.class);

        for (Map.Entry<TrackFlag, FlagControls> e : flags.entrySet()) {
            if (e.getValue().box.isSelected()) {
                selected.put(e.getKey(), e.getValue().yes.isSelected());
            }
        }

        return selected;
    }

    /** Lays out the card: 2 columns; the flags share one two-column block. */
    private void buildCard(MkvStrings mkvStrings) {
        GridBagLayout layout = new GridBagLayout();
        layout.columnWidths = new int[] { 0, 0, 0 };
        layout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
        layout.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
        layout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        card.setLayout(layout);

        // Row 0: Edit this track (the only checkbox enabled from the start).
        addCell(chbEdit, 0, 0, new Insets(0, 0, 10, 5), GridBagConstraints.NONE,
                GridBagConstraints.WEST);

        // Row 1: all flags, four per column, each with its own Yes/No group.
        JPanel flagGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbcFlags = new GridBagConstraints();
        gbcFlags.gridx = 0;
        gbcFlags.gridy = 1;
        gbcFlags.gridwidth = 2;
        gbcFlags.anchor = GridBagConstraints.WEST;
        gbcFlags.insets = new Insets(0, 0, 5, 0);
        card.add(flagGrid, gbcFlags);

        int half = (TrackFlag.values().length + 1) / 2;
        int i = 0;
        for (FlagControls controls : flags.values()) {
            addFlagCell(flagGrid, controls, (i / half) * 2, i % half);
            i++;
        }

        // Row 2: track name.
        chbName.setEnabled(false);
        addCell(chbName, 0, 2, new Insets(0, 0, 5, 5), GridBagConstraints.NONE,
                GridBagConstraints.WEST);
        txtName.setEnabled(false);
        addCell(txtName, 1, 2, new Insets(0, 0, 5, 0), GridBagConstraints.HORIZONTAL,
                GridBagConstraints.CENTER);
        txtName.setColumns(10);

        // Row 3: numbering controls (checkbox + start/pad fields in one panel).
        JPanel numbControls = new JPanel();
        FlowLayout numbLayout = (FlowLayout) numbControls.getLayout();
        numbLayout.setAlignment(FlowLayout.LEFT);
        numbLayout.setVgap(0);
        addCell(numbControls, 1, 3, new Insets(0, 0, 5, 0), GridBagConstraints.BOTH,
                GridBagConstraints.CENTER);

        chbNumb.setEnabled(false);
        numbControls.add(chbNumb);
        numbControls.add(Box.createHorizontalStrut(10));

        lblNumbStart.setEnabled(false);
        numbControls.add(lblNumbStart);
        txtNumbStart.setText("1");
        txtNumbStart.setEnabled(false);
        txtNumbStart.setColumns(10);
        numbControls.add(txtNumbStart);

        numbControls.add(Box.createHorizontalStrut(5));

        lblNumbPad.setEnabled(false);
        numbControls.add(lblNumbPad);
        txtNumbPad.setText("1");
        txtNumbPad.setEnabled(false);
        txtNumbPad.setColumns(10);
        numbControls.add(txtNumbPad);

        // Row 4: the numbering explanation.
        lblNumbExplain.setEnabled(false);
        addCell(lblNumbExplain, 1, 4, new Insets(0, 0, 10, 0), GridBagConstraints.NONE,
                GridBagConstraints.NORTHWEST);

        // Row 5: language list, or a custom BCP 47 tag that overrides it.
        chbLang.setEnabled(false);
        addCell(chbLang, 0, 5, new Insets(0, 0, 10, 5), GridBagConstraints.NONE,
                GridBagConstraints.WEST);

        JPanel langControls = new JPanel();
        FlowLayout langLayout = (FlowLayout) langControls.getLayout();
        langLayout.setAlignment(FlowLayout.LEFT);
        langLayout.setVgap(0);
        langLayout.setHgap(0);
        addCell(langControls, 1, 5, new Insets(0, 0, 10, 0), GridBagConstraints.HORIZONTAL,
                GridBagConstraints.WEST);

        cbLang.setEnabled(false);
        cbLang.setModel(new DefaultComboBoxModel<String>(mkvStrings.getLangNames()));
        cbLang.setSelectedIndex(mkvStrings.getLangCodeList().indexOf("und"));
        langControls.add(cbLang);
        langControls.add(Box.createHorizontalStrut(10));

        String tagTip = "Optional IETF BCP 47 tag such as en-US, pt-BR, es-419 or zh-Hant."
                + " When filled in, it is used instead of the list.";
        lblLangTag.setEnabled(false);
        lblLangTag.setToolTipText(tagTip);
        langControls.add(lblLangTag);
        langControls.add(Box.createHorizontalStrut(5));
        txtLangTag.setEnabled(false);
        txtLangTag.setColumns(8);
        txtLangTag.setToolTipText(tagTip);
        langControls.add(txtLangTag);

        // Row 6: extra parameters (no bottom gap, like the original).
        chbExtra.setEnabled(false);
        addCell(chbExtra, 0, 6, new Insets(0, 0, 0, 0), GridBagConstraints.NONE,
                GridBagConstraints.WEST);
        txtExtra.setEnabled(false);
        addCell(txtExtra, 1, 6, new Insets(0, 0, 0, 0), GridBagConstraints.HORIZONTAL,
                GridBagConstraints.CENTER);
        txtExtra.setColumns(10);

        /* Start of mouse events for right-click menu */

        Utils.addRCMenuMouseListener(txtName);
        Utils.addRCMenuMouseListener(txtNumbStart);
        Utils.addRCMenuMouseListener(txtNumbPad);
        Utils.addRCMenuMouseListener(txtLangTag);
        Utils.addRCMenuMouseListener(txtExtra);

        /* End of mouse events for right-click menu */
    }

    /** Wires the enable/disable dance, one copy for every track type. */
    private void wireListeners() {
        chbEdit.addActionListener(e -> {
            boolean state = chbDefault.isEnabled();

            for (FlagControls f : flags.values()) {
                f.box.setEnabled(!state);

                if (f.no.isEnabled() || f.box.isSelected()) {
                    f.no.setEnabled(!state);
                    f.yes.setEnabled(!state);
                }
            }

            chbName.setEnabled(!state);
            chbLang.setEnabled(!state);
            chbExtra.setEnabled(!state);

            if (txtName.isEnabled() || chbName.isSelected()) {
                txtName.setEnabled(!state);
                chbNumb.setEnabled(!state);

                if (chbNumb.isSelected()) {
                    lblNumbStart.setEnabled(!state);
                    txtNumbStart.setEnabled(!state);
                    lblNumbPad.setEnabled(!state);
                    txtNumbPad.setEnabled(!state);
                    lblNumbExplain.setEnabled(!state);
                }
            }

            if (cbLang.isEnabled() || chbLang.isSelected()) {
                setLanguageControlsEnabled(!state);
            }

            if (txtExtra.isEnabled() || chbExtra.isSelected()) {
                chbExtra.setEnabled(!state);
                txtExtra.setEnabled(!state);
            }
        });

        for (FlagControls f : flags.values()) {
            f.box.addActionListener(e -> {
                boolean state = f.no.isEnabled();
                f.no.setEnabled(!state);
                f.yes.setEnabled(!state);
            });
        }

        chbName.addActionListener(e -> {
            boolean state = chbNumb.isEnabled();
            chbNumb.setEnabled(!state);
            txtName.setEnabled(!state);

            if (chbNumb.isSelected()) {
                lblNumbStart.setEnabled(!state);
                txtNumbStart.setEnabled(!state);
                lblNumbPad.setEnabled(!state);
                txtNumbPad.setEnabled(!state);
                lblNumbExplain.setEnabled(!state);
            }
        });

        chbNumb.addActionListener(e -> {
            boolean state = txtNumbStart.isEnabled();
            lblNumbStart.setEnabled(!state);
            txtNumbStart.setEnabled(!state);
            lblNumbPad.setEnabled(!state);
            txtNumbPad.setEnabled(!state);
            lblNumbExplain.setEnabled(!state);
        });

        txtNumbStart.addFocusListener(validateNumberingOnFocusLost(txtNumbStart));
        txtNumbPad.addFocusListener(validateNumberingOnFocusLost(txtNumbPad));

        chbLang.addActionListener(e -> setLanguageControlsEnabled(!cbLang.isEnabled()));

        chbExtra.addActionListener(e -> {
            boolean state = txtExtra.isEnabled();
            txtExtra.setEnabled(!state);
        });
    }

    private void setLanguageControlsEnabled(boolean enabled) {
        cbLang.setEnabled(enabled);
        lblLangTag.setEnabled(enabled);
        txtLangTag.setEnabled(enabled);
    }

    /** Negative or unparseable numbering values fall back to "1" on focus loss. */
    private static FocusListener validateNumberingOnFocusLost(JTextField field) {
        return new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    if (Integer.parseInt(field.getText()) < 0) {
                        field.setText("1");
                    }
                } catch (NumberFormatException e1) {
                    field.setText("1");
                }
            }
        };
    }

    /**
     * Adds one flag to the flag grid: the checkbox, then a left-aligned Yes/No
     * radio group. Radios start disabled with Yes selected.
     */
    private static void addFlagCell(JPanel grid, FlagControls f, int column, int row) {
        f.box.setEnabled(false);
        GridBagConstraints gbcBox = new GridBagConstraints();
        gbcBox.gridx = column;
        gbcBox.gridy = row;
        gbcBox.anchor = GridBagConstraints.WEST;
        gbcBox.insets = new Insets(0, column == 0 ? 0 : 20, 3, 5);
        grid.add(f.box, gbcBox);

        JPanel controls = new JPanel();
        FlowLayout layout = (FlowLayout) controls.getLayout();
        layout.setAlignment(FlowLayout.LEFT);
        layout.setVgap(0);
        GridBagConstraints gbcControls = new GridBagConstraints();
        gbcControls.gridx = column + 1;
        gbcControls.gridy = row;
        gbcControls.anchor = GridBagConstraints.WEST;
        gbcControls.insets = new Insets(0, 0, 3, 0);
        grid.add(controls, gbcControls);

        f.yes.setEnabled(false);
        f.yes.setSelected(true);
        controls.add(f.yes);

        f.no.setEnabled(false);
        controls.add(f.no);

        ButtonGroup group = new ButtonGroup();
        group.add(f.yes);
        group.add(f.no);
    }

    /** Places one component on the card's grid. */
    private void addCell(Component component, int column, int row, Insets insets, int fill,
            int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = insets;
        gbc.fill = fill;
        gbc.anchor = anchor;
        gbc.gridx = column;
        gbc.gridy = row;
        card.add(component, gbc);
    }
}
