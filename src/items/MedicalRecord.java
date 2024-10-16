package items;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The MedicalRecord class holds comprehensive medical information about a patient.
 */
public class MedicalRecord {
    private String patientID;
    private String name;
    private LocalDate dateOfBirth;
    private String gender;
    private ContactInformation contactInformation;
    private String bloodType;
    private List<Diagnosis> pastDiagnoses;
    private List<Treatment> pastTreatments;
    private String assignedDoctorId;

    /**
     * Constructor for MedicalRecord.
     *
     * @param patientID          Unique identifier for the patient
     * @param name               Full name of the patient
     * @param dateOfBirth        Date of birth
     * @param gender             Gender of the patient
     * @param contactInformation Contact information (phone number, email)
     */
    public MedicalRecord(String patientID, String name, LocalDate dateOfBirth, String gender,
                        ContactInformation contactInformation) {
        this.patientID = patientID;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.contactInformation = contactInformation;
        this.bloodType = "";
        this.pastDiagnoses = new ArrayList<>();
        this.pastTreatments = new ArrayList<>();
        
    }

    /**
     * Overloaded constructor with additional fields.
     *
     * @param patientID          Unique identifier for the patient
     * @param name               Full name of the patient
     * @param dateOfBirth        Date of birth
     * @param gender             Gender of the patient
     * @param contactInformation Contact information (phone number, email)
     * @param bloodType          Blood type of the patient
     * @param pastDiagnoses      List of past diagnoses
     * @param pastTreatments     List of past treatments
     */
    public MedicalRecord(String patientID, String name, LocalDate dateOfBirth, String gender,
                        ContactInformation contactInformation, String bloodType,
                        List<Diagnosis> pastDiagnoses, List<Treatment> pastTreatments) {
        this.patientID = patientID;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.contactInformation = contactInformation;
        this.bloodType = bloodType;
        this.pastDiagnoses = pastDiagnoses;
        this.pastTreatments = pastTreatments;
        this.assignedDoctorId = null; // Initially no doctor assigned
    }

    // Getters and Setters

    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) { 
        this.patientID = patientID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) { 
        this.name = name;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) { 
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) { 
        this.gender = gender;
    }

    public ContactInformation getContactInformation() {
        return contactInformation;
    }

    public void setContactInformation(ContactInformation contactInformation) { 
        this.contactInformation = contactInformation;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) { 
        this.bloodType = bloodType;
    }

    public List<Diagnosis> getPastDiagnoses() {
        return pastDiagnoses;
    }

    public String getAssignedDoctorId() {
        return assignedDoctorId;
    }

    public void setAssignedDoctorId(String assignedDoctorId) {
        this.assignedDoctorId = assignedDoctorId;
    }

    /**
     * Adds a diagnosis to the patient's medical record.
     *
     * @param diagnosis Diagnosis object to add
     */
    public void addDiagnosis(Diagnosis diagnosis) {
        this.pastDiagnoses.add(diagnosis);
    }

    public List<Treatment> getPastTreatments() {
        return pastTreatments;
    }

    /**
     * Adds a treatment to the patient's medical record.
     *
     * @param treatment Treatment object to add
     */
    public void addTreatment(Treatment treatment) {
        this.pastTreatments.add(treatment);
    }

    /**
     * Adds a prescription to the patient's medical record.
     * This assumes each prescription is part of a treatment.
     *
     * @param prescription Prescription object to add
     */
    public void addPrescription(Prescription prescription) {
        // Find or create a treatment to add the prescription
        if (this.pastTreatments.isEmpty()) {
            Treatment treatment = new Treatment();
            treatment.addPrescription(prescription);
            this.pastTreatments.add(treatment);
        } else {
            // For simplicity, add to the last treatment
            this.pastTreatments.get(this.pastTreatments.size() - 1).addPrescription(prescription);
        }
    }

    @Override
    public String toString() {
        return "MedicalRecord{" +
                "patientID='" + patientID + '\'' +
                ", name='" + name + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", gender='" + gender + '\'' +
                ", contactInformation=" + contactInformation +
                ", bloodType='" + bloodType + '\'' +
                ", pastDiagnoses=" + pastDiagnoses +
                ", pastTreatments=" + pastTreatments +
                '}';
    }

    // Additional methods can be added here as needed
}
