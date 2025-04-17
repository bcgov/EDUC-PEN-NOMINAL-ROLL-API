package ca.bc.gov.educ.pen.nominalroll.api.model.v1;

public class AssignedPenEntity {
    private String assignedPen;

    public AssignedPenEntity(String assignedPen) {
        this.assignedPen = assignedPen;
    }

    public String getAssignedPen() {
        return assignedPen;
    }

    public void setAssignedPen(String assignedPen) {
        this.assignedPen = assignedPen;
    }
}
