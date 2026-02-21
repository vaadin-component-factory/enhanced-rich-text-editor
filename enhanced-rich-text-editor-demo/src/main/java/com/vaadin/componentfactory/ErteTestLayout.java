/*-
 * #%L
 * Enhanced Rich Text Editor V25 Demo
 * %%
 * Copyright (C) 2019 - 2025 Vaadin Ltd
 * %%
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file license.html distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the CVALv3 along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 * #L%
 */
package com.vaadin.componentfactory;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;

/**
 * Auto-layout for all ERTE test views under {@code /erte-test/*}.
 * <p>
 * Provides a side navigation organized by migration phase/feature tier.
 */
@Layout("erte-test")
public class ErteTestLayout extends AppLayout {

    public ErteTestLayout() {
        var toggle = new DrawerToggle();

        var title = new H1("ERTE Test Views");
        title.getStyle()
                .set("font-size", "1rem")
                .set("margin", "0");

        addToNavbar(toggle, title);

        var nav = createSideNav();
        var scroller = new Scroller(nav);
        scroller.getStyle().set("padding", "var(--lumo-space-s)");
        addToDrawer(scroller);

        setPrimarySection(Section.DRAWER);
    }

    private SideNav createSideNav() {
        var nav = new SideNav();

        // -- Tier 1: Core Differentiators --
        var tier1 = new SideNavItem("Tier 1 — Core");
        tier1.setPrefixComponent(VaadinIcon.STAR.create());

        var toolbarItem = new SideNavItem("Toolbar",
                "erte-test/toolbar",
                VaadinIcon.TOOLS.create());
        toolbarItem.addItem(new SideNavItem("3.1a Slots"));
        toolbarItem.addItem(new SideNavItem("3.2a Visibility"));
        toolbarItem.addItem(new SideNavItem("3.2b Shortcuts"));
        tier1.addItem(toolbarItem);

        tier1.addItem(new SideNavItem("3.1b Readonly",
                "erte-test/readonly",
                VaadinIcon.LOCK.create()));

        tier1.addItem(new SideNavItem("3.1c Tabstops",
                "erte-test/tabstops",
                VaadinIcon.ELLIPSIS_DOTS_H.create()));

        tier1.addItem(new SideNavItem("3.1f Placeholders",
                "erte-test/placeholders",
                VaadinIcon.INPUT.create()));

        tier1.addItem(new SideNavItem("3.1g extendOptions",
                "erte-test/extend-options",
                VaadinIcon.COG.create()));

        // -- Tier 3: Remaining --
        var tier3 = new SideNavItem("Tier 3 — Remaining");
        tier3.setPrefixComponent(VaadinIcon.STAR_O.create());

        tier3.addItem(new SideNavItem("3.3a NBSP",
                "erte-test/features", VaadinIcon.TEXT_LABEL.create()));

        tier3.addItem(new SideNavItem("3.3b Whitespace",
                "erte-test/tabstops",
                VaadinIcon.ALIGN_JUSTIFY.create()));

        tier3.addItem(disabled("3.3c Sanitizer",
                VaadinIcon.SHIELD.create()));

        tier3.addItem(disabled("3.3d I18n",
                VaadinIcon.GLOBE.create()));

        tier3.addItem(disabled("3.3e addText",
                VaadinIcon.PENCIL.create()));

        tier3.addItem(new SideNavItem("3.3f Align Justify",
                "erte-test/align-justify",
                VaadinIcon.ALIGN_JUSTIFY.create()));

        tier3.addItem(new SideNavItem("3.3g Replace Icons",
                "erte-test/replace-icons",
                VaadinIcon.PICTURE.create()));

        tier3.addItem(disabled("3.3h Arrow Nav",
                VaadinIcon.ARROWS_LONG_H.create()));

        // -- Phase 3.4: Open Issues --
        var phase34 = new SideNavItem("Phase 3.4 — Open Issues");
        phase34.setPrefixComponent(VaadinIcon.WRENCH.create());

        phase34.addItem(new SideNavItem("3.4g ToolbarPopover",
                "erte-test/toolbar-popover",
                VaadinIcon.VIEWPORT.create()));

        phase34.addItem(new SideNavItem("3.4h ToolbarSelectPopup",
                "erte-test/toolbar-select-popup",
                VaadinIcon.MENU.create()));

        nav.addItem(tier1, tier3, phase34);
        return nav;
    }

    /**
     * Creates a disabled (non-navigable) SideNavItem for not-yet-implemented
     * features.
     */
    private static SideNavItem disabled(String label,
            com.vaadin.flow.component.Component icon) {
        var item = new SideNavItem(label);
        item.setPrefixComponent(icon);
        var badge = new Span("TODO");
        badge.getElement().getThemeList().add("badge contrast pill");
        badge.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
        item.setSuffixComponent(badge);
        return item;
    }
}
