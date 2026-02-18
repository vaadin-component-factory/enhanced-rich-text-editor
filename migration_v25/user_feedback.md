Ich hab mir den aktuellen Stand in V25 angeschaut. Hier ein erstes einfaches Feedback. Bitte evaluieren, mit deinen eigenen Infos/Stand/Memory abgleichen
und mit mir besprechen.

# issues allgemein
* demos 25 initial content funktioniert nicht
* Javascript fehler mit irgendeinem parchment
* toolbar stylings "broken", sehen nicht aus wie der RTE. auch die table buttons haben andere stylings
* tab whitespaces werden scheinbar nur angezeigt, wenn tabstops aktiv sind (keine update regression, hat auch in V24 scheinbar so funktioniert, muss aber dennoch gefixt werden)
* placeholder toolbar buttons komplett schwarz
* placeholder funktioniert nicht? Das popup öffnet sich nicht - hab hier was von JS Popu gelesen. Bitte funktionalität mit normalem Popover wiederherstellen
* 
* readonly macht scheinbar nichts, ich kann trotzdem schreiben -> Uncaught r: [Parchment] Cannot wrap readonly
  at i.wrap (@vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:3780:132)
  at i.formatAt (@vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:3742:64)
  at @vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:3960:16
  at b.forEachAt (@vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:3880:69)
  at a.formatAt (@vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:3959:25)
  at a.formatAt (@vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:4114:81)
  at a.formatAt (@vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:900:120)
  at @vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:3960:16
  at b.forEachAt (@vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:3880:69)
  at m.formatAt (@vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:3959:25)
  at m.formatAt (@vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:4197:32)
  at m.formatAt (@vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:1184:17)
  at @vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:1810:25
  at Array.forEach (<anonymous>)
  at A.formatText (@vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:1809:27)
  at @vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:2057:92
  at B.U (@vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:2195:44)
  at B.formatText (@vaadin_rich-text-editor_src_vaadin-rich-text-editor__js.js?v=3f60eff2:2057:62)
  at EnhancedRichTextEditor._onReadonlyClick (vcf-enhanced-rich-text-editor.js:1459:20)

# general
* die toolbar ist ja nun doch komplett kopiert im render. eigentlich war die idee, das nicht zu machen - keine bessere variante gefunden (ehrlich gemeinte Frage, bitte erläutern)?
- die Idee mit "wir rendern unsere slots in den bestehenden teil rein" würde ich gern generischer / automatisierter sehen
- bitte prüfen, ob möglich, dass man das render result von super.render() nimmt, auf elemente mit part*="toolbar-group" scannt und hier dann davor
und danach passende slots einfügt. 
- Toolbaranpassung ist etwas für später, ERSTMAL GRUNDFUNKTIONALITÄT HERSTELLEN
* manche maven profile sind komplett rausgeflogen aus der pom?
* Projektfiles (keine src files in dem sinne), die in 24 existieren aber nicht in 25, z.B. readme.md in den tables

# packages
* dass der ERTE in einem eigenen Package sitzt, gefällt mir nicht. du meintest, dass private-package hier schuld ist. dennoch müssen wir 
eine alternative finden, da jetzt die package strutkrue zerrissen ist. ggf. im flow package eine protected zwischen klasse, die das package-protected
auf protected anhebt und der ERTE in seinem original package dann davon erbt? RTE -> RteExtensionBase oder so -> ERTE

# stylesheets
* erte stylings sind noch in einem veralteten format deklariert, das noch aus polymerzeiten stammt wenn ich mich nicht irre
* Vaadinkomponenten machen das so eigentlich nicht mehr (iirc)
* hier müsste mal analyisert werden, was hier der state of the art ist und entsprechend anpassen (stylesheet datei, lumo spezifische imports)
* Aura styling müssen wir dann auch nochmal angehen, aber später


