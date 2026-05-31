package com.example.incidentmap;

public class Incident {
    private IncidentEntity entity;
    private String address;

    public Incident(IncidentEntity entity, String address) {
        this.entity = entity;
        this.address = address;
    }

    public IncidentEntity getEntity() { return entity; }
    public String getAddress() { return address; }
    public String getTypeIcon() {
        switch (entity.type) {
            case "accident": return "🚗";
            case "repair": return "🚧";
            case "hole": return "🕳";
            default: return "⚠";
        }
    }
}