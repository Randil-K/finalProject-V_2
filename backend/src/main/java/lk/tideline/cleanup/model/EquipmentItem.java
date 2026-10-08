package lk.tideline.cleanup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.ColumnDefault;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One line of equipment a cleanup needs, e.g. "Gloves", 40, and how much of it people have pledged. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentItem {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int quantity;

    /** How much of this line volunteers have promised to bring. */
    @Column(nullable = false)
    @ColumnDefault("0")
    private int securedQuantity;

    public EquipmentItem(String name, int quantity) {
        this(name, quantity, 0);
    }

    public boolean isSecured() {
        return securedQuantity >= quantity;
    }
}
