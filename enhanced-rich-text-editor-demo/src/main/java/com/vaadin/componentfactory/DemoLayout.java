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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.PageTitle;

/**
 * Root layout for the ERTE V25 demo application.
 * <p>
 * Provides side navigation with links to the playground views and a
 * dark/light mode toggle in the navbar.
 */
@Layout
public class DemoLayout extends AppLayout implements AfterNavigationObserver {

    private boolean darkMode = false;
    private final H1 viewTitle = new H1();

    public DemoLayout() {
        var toggle = new DrawerToggle();

        viewTitle.getStyle()
                .set("font-size", "1rem")
                .set("margin", "0");

        var themeToggle = new Button(VaadinIcon.MOON.create(),
                e -> toggleColorScheme(e.getSource()));
        themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeToggle.getElement().setAttribute("aria-label",
                "Toggle dark/light mode");

        var navbar = new HorizontalLayout(toggle, viewTitle, themeToggle);
        navbar.setWidthFull();
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        navbar.expand(viewTitle);
        addToNavbar(navbar);

        // Drawer: heading + navigation
        var heading = new H2("Vaadin Enhanced RTE");
        heading.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");

        var nav = createSideNav();

        var drawer = new VerticalLayout(heading, nav);
        drawer.setPadding(true);
        drawer.setSpacing(true);
        addToDrawer(drawer);

        setPrimarySection(Section.DRAWER);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        // Update navbar title from the current view's @PageTitle
        var activeChains = event.getActiveChain();
        if (!activeChains.isEmpty()) {
            var viewClass = activeChains.getFirst().getClass();
            var pageTitle = viewClass.getAnnotation(PageTitle.class);
            viewTitle.setText(
                    pageTitle != null ? pageTitle.value() : "");
        }
    }

    private SideNav createSideNav() {
        var nav = new SideNav();

        nav.addItem(new SideNavItem("Enhanced RTE Playground",
                ErtePlaygroundView.class));

        nav.addItem(new SideNavItem("RTE Playground",
                RtePlaygroundView.class));

        nav.addItem(new SideNavItem("Enhanced RTE Samples",
                ErteSamplesView.class));

        return nav;
    }

    private void toggleColorScheme(Button button) {
        darkMode = !darkMode;
        var page = UI.getCurrent().getPage();
        if (darkMode) {
            page.setColorScheme(ColorScheme.Value.DARK);
            button.setIcon(VaadinIcon.SUN_O.create());
        } else {
            page.setColorScheme(ColorScheme.Value.LIGHT);
            button.setIcon(VaadinIcon.MOON.create());
        }
    }
}
