package com.vaadin.componentfactory;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.dom.Element;

public class SlotUtil {

    private static String SLOTNAME = "toolbar";
    
    private static Stream<Element> getElementsInSlot(HasElement target,
            String slot) {
        return target.getElement().getChildren()
                .filter(child -> slot.equals(child.getAttribute("slot")));
    }

    public static void addButton(EnhancedRichTextEditor target, Button component) {
//        clearSlot(target,SLOTNAME);

        if (component != null) {
            component.getElement().setAttribute("slot", SLOTNAME);
            target.getElement().appendChild(component.getElement());
        }
    }

    private static void clearSlot(EnhancedRichTextEditor target, String slot) {
        getElementsInSlot(target, slot).collect(Collectors.toList())
                .forEach(target.getElement()::removeChild);
    }

    private static Stream<Button> getButtonsInSlot(HasElement target, String slot) {
        Stream<Element> elements = getElementsInSlot(target, slot);
        Stream<Button> buttons = elements
                .filter(element -> element.getComponent().isPresent())
                .map(element -> (Button) element.getComponent().get());
        return buttons;
    }

    public static void removeButton(EnhancedRichTextEditor target, String id) {
        Stream<Button> buttons = getButtonsInSlot(target, SLOTNAME);
        Optional<Button> match = buttons.filter(button -> button.getId().equals(id)).findFirst();
        if (match.isPresent()) {
            target.getElement().removeChild(match.get().getElement());
        }
    }

    public static void removeButton(EnhancedRichTextEditor target, Button button) {
        Stream<Button> buttons = getButtonsInSlot(target, SLOTNAME);
        Optional<Button> match = buttons.filter(btn -> btn.equals(button)).findFirst();
        if (match.isPresent()) {
            target.getElement().removeChild(match.get().getElement());
        }
    }

    public static Button getButton(EnhancedRichTextEditor target, String id) {
        Stream<Button> buttons = getButtonsInSlot(target, SLOTNAME);
        Optional<Button> match = buttons.filter(button -> button.getId().equals(id)).findFirst();
        if (match.isPresent()) {
            return match.get();
        } else {
            return null;
        }
    }
    
	public static void replaceStandardButtonIcon(EnhancedRichTextEditor target, Icon icon, String iconSlotName) {
		if (icon != null) {
			clearSlot(target, iconSlotName);
			icon.getElement().setAttribute("slot", iconSlotName);
			target.getElement().appendChild(icon.getElement());
		}
	}
}
