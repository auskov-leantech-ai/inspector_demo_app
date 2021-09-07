package www.info_pro.ru.infrastructure;

/* Элемент  меню */
public class AppDialogMenuItem {
    public final String text;
    public final int icon;

    public AppDialogMenuItem(String text, Integer icon) {
        this.text = text;
        this.icon = icon;
    }

    @Override
    public String toString() {
        return text;
    }
}

