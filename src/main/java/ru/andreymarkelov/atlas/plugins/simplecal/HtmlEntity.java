package ru.andreymarkelov.atlas.plugins.simplecal;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HtmlEntity {
    @XmlElement
    private String html;

    public HtmlEntity(String html) {
        this.html = html;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    @Override
    public String toString() {
        return ("HtmlEntity[html=" + html + "]");
    }
}
