package com.example.fff.model;
import java.util.List;
import com.example.fff.api.WareneingangInterface;

/**
 * The Wareneingang class represents an inbound goods receipt process in an inventory or warehouse system.
 * It encapsulates the details of a specific goods receipt event, including its identifier, receipt date,
 * list of items received, and the mode of receipt.
 *
 * This class implements the WareneingangInterface, ensuring a standardized structure for accessing
 * and managing the attributes of a goods receipt.
 */
public class Wareneingang implements WareneingangInterface {


        private int wareneingangId;
        private String wareneingangDate;
        private List<WareneingangItem> items;
    private String wareneingangMode;
    public Wareneingang(int wareneingangId, String wareneingangDate, List<WareneingangItem> items, String wareneingangMode) {
            this.wareneingangId = wareneingangId;
            this.wareneingangDate = wareneingangDate;
            this.items = items;
            this.wareneingangMode = wareneingangMode;
        }
@Override
public int getWareneingangId() {
            return wareneingangId;
        }
@Override
public String getWareneingangDate() {
            return wareneingangDate;
        }

        @Override
        public List<WareneingangItem> getItems() {
            return items;
        }

        @Override
    public String getWareneingangMode() {
        return wareneingangMode;
    }

    @Override
    public void setWareneingangMode(String wareneingangMode) {
        this.wareneingangMode = wareneingangMode;
    }
}
