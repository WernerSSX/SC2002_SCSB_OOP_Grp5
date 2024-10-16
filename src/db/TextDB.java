package db;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import user_classes.*;
import items.*;

public class TextDB {
    private static TextDB instance;
    private List<MedicalRecord> medicalRecords;
    public static final String SEPARATOR = "|";
    private static List<User> users;
    private static List<Appointment> appointments;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private TextDB() {
        users = new ArrayList<>();
        appointments = new ArrayList<>();
        medicalRecords = new ArrayList<>();
    }

    public static TextDB getInstance() {
        if (instance == null) {
            instance = new TextDB();
            try {
                instance.loadAllData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    /**
     * Loads all data including users, appointments, medical records, and schedules.
     */
    private void loadAllData() throws IOException {
        loadFromFile("users.txt");
        loadAppointmentsFromFile("appts.txt");
        loadMedicalRecordsFromFile("med_records.txt");
        loadSchedulesFromFile("schedules.txt");
    }

    /**
     * Saves all data including users, appointments, medical records, and schedules.
     */
    public void saveAllData() throws IOException {
        saveToFile("users.txt");
        saveAppointmentsToFile("appts.txt");
        saveMedicalRecordsToFile("med_records.txt");
        saveSchedulesToFile("schedules.txt");
    }

    // Existing methods for Users, Appointments, and MedicalRecords...

    // ====================== Schedule Management ========================= //

    /**
     * Loads schedules from the specified file and assigns them to respective doctors.
     *
     * @param filename The name of the schedules file.
     * @throws IOException If an I/O error occurs.
     */
    public void loadSchedulesFromFile(String filename) throws IOException {
        List<String> lines = read(filename);
        for (String line : lines) {
            String[] fields = line.split("\\" + SEPARATOR);
            if (fields.length < 3) {
                System.err.println("Invalid schedule entry: " + line);
                continue;
            }
    
            String doctorId = fields[0];
            LocalDate date = LocalDate.parse(fields[1], DATE_FORMATTER);
            String timeSlotsStr = fields[2];
    
            List<TimeSlot> timeSlots = new ArrayList<>();
            if (!timeSlotsStr.isEmpty()) {
                String[] slots = timeSlotsStr.split(",");
                for (String slot : slots) {
                    String[] times = slot.split("-");
                    if (times.length != 2) {
                        System.err.println("Invalid time slot format: " + slot);
                        continue;
                    }
                    LocalTime startTime = LocalTime.parse(times[0], TIME_FORMATTER);
                    LocalTime endTime = LocalTime.parse(times[1], TIME_FORMATTER);
                    TimeSlot timeSlot = new TimeSlot(
                            LocalDateTime.of(date, startTime),
                            LocalDateTime.of(date, endTime),
                            true
                    );
                    timeSlots.add(timeSlot);
                }
            }
    
            // Assign the time slots to the doctor's schedule
            Doctor doctor = (Doctor) getUserByHospitalID(doctorId);
            if (doctor != null) {
                doctor.getSchedule().setAvailability(date, timeSlots);
            } else {
                System.err.println("Doctor with ID " + doctorId + " not found.");
            }
        }
    }
    
    

    /**
     * Saves all doctors' schedules to the specified file.
     *
     * @param filename The name of the schedules file.
     * @throws IOException If an I/O error occurs.
     */
    public void saveSchedulesToFile(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        for (User user : users) {
            if (user instanceof Doctor) {
                Doctor doctor = (Doctor) user;
                Schedule schedule = doctor.getSchedule();
                for (LocalDate date : schedule.getAvailability().keySet()) {
                    List<TimeSlot> slots = schedule.getAvailableTimeSlots(date);
                    StringBuilder slotsStr = new StringBuilder();
                    for (int i = 0; i < slots.size(); i++) {
                        TimeSlot slot = slots.get(i);
                        slotsStr.append(slot.getStartTime().toLocalTime().format(TIME_FORMATTER))
                                .append("-")
                                .append(slot.getEndTime().toLocalTime().format(TIME_FORMATTER));
                        if (i < slots.size() - 1) {
                            slotsStr.append(",");
                        }
                    }
                    String line = String.join(SEPARATOR,
                            doctor.getHospitalID(),
                            date.format(DATE_FORMATTER),
                            slotsStr.toString()
                    );
                    lines.add(line);
                }
            }
        }
        write(filename, lines);
    }

    /**
     * Updates a doctor's schedule and saves the changes to the schedules file.
     *
     * @param doctorId The ID of the doctor whose schedule is to be updated.
     * @param schedule The updated Schedule object.
     * @throws IOException If an I/O error occurs.
     */
    public void updateDoctorSchedule(String doctorId, Schedule schedule) throws IOException {
        Doctor doctor = (Doctor) getUserByHospitalID(doctorId);
        if (doctor != null) {
            doctor.setSchedule(schedule);
            saveSchedulesToFile("schedules.txt");
        } else {
            System.err.println("Doctor with ID " + doctorId + " not found.");
        }
    }

    // ====================== Existing Methods ========================= //

    public void addUser(User user) {
        users.add(user);
    }

    public void removeUser(User user) {
        users.remove(user);
    }

    public User getUserByHospitalID(String hospitalID) {
        for (User user : users) {
            if (user.getHospitalID().equals(hospitalID)) {
                return user;
            }
        }
        return null;
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public static void updateUserPassword(User user) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getHospitalID().equals(user.getHospitalID())) {
                users.set(i, user);
                break;
            }
        }
        try {
            saveToFile("users.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveToFile(String filename) throws IOException {
        List<String> stringList = new ArrayList<>();
        for (User user : users) {
            stringList.add(serializeUser(user));
        }
        write(filename, stringList);
    }

    public void loadFromFile(String filename) throws IOException {
        List<String> stringArray = read(filename);
        users.clear();
        for (String line : stringArray) {
            users.add(deserializeUser(line));
        }
    }

    private static String serializeUser(User user) {
        return String.join(SEPARATOR,
                user.getHospitalID(),
                user.getPassword(),
                user.getName(),
                user.getDateOfBirth().format(DATE_FORMATTER),
                user.getGender(),
                user.getContactInformation().getPhoneNumber(),
                user.getContactInformation().getEmailAddress(),
                user.getRole());
    }

    private User deserializeUser(String userData) {
        String[] fields = userData.split("\\" + SEPARATOR);
        if (fields.length < 8) {
            throw new IllegalArgumentException("Invalid user data: " + userData);
        }

        String hospitalID = fields[0];
        String password = fields[1];
        String name = fields[2];
        LocalDate dateOfBirth = LocalDate.parse(fields[3], DATE_FORMATTER);
        String gender = fields[4];
        String phone = fields[5];
        String email = fields[6];
        String role = fields[7].toLowerCase();

        ContactInformation contactInformation = new ContactInformation(phone, email);

        switch (role) {
            case "administrator":
                return new Administrator(hospitalID, password, name, dateOfBirth, gender, contactInformation);
            case "doctor":
                return new Doctor(hospitalID, password, name, dateOfBirth, gender, contactInformation, new Schedule());
            case "patient":
                return new Patient(hospitalID, password, name, dateOfBirth, gender, contactInformation);
            case "pharmacist":
                return new Pharmacist(hospitalID, password, name, dateOfBirth, gender, contactInformation);
            default:
                throw new IllegalArgumentException("Unknown role: " + role);
        }
    }

    public static void write(String fileName, List<String> data) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(fileName));
        try {
            for (String line : data) {
                out.println(line);
            }
        } finally {
            out.close();
        }
    }

    public static List<String> read(String fileName) throws IOException {
        List<String> data = new ArrayList<>();
        FileInputStream fis = null;
        Scanner scanner = null;
        try {
            fis = new FileInputStream(fileName);
            scanner = new Scanner(fis);
            while (scanner.hasNextLine()) {
                data.add(scanner.nextLine());
            }
        } finally {
            if (scanner != null) {
                scanner.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
        return data;
    }

    // ====================== Appointment Management ========================= //

    public boolean cancelAppointment(Patient patient, int appointmentId) {
        boolean removed = appointments.removeIf(appointment -> 
            appointment.getId() == appointmentId && appointment.getPatientId().equals(patient.getHospitalID())
        );

        if (removed) {
            try {
                saveAppointmentsToFile("appts.txt");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return removed;
    }

    private int generateNewAppointmentId() {
        return appointments.stream()
                           .mapToInt(Appointment::getId)
                           .max()
                           .orElse(0) + 1;
    }

    public List<Doctor> getAllDoctors() {
        return users.stream()
                    .filter(user -> user instanceof Doctor)
                    .map(user -> (Doctor) user)
                    .collect(Collectors.toList());
    }

    public List<TimeSlot> getAvailableAppointmentSlots(LocalDate date, Doctor doctor) {
        // Step 1: Retrieve the doctor's available slots for the date
        List<TimeSlot> doctorAvailableSlots = doctor.getAvailableTimeSlots(date);
        
        if (doctorAvailableSlots.isEmpty()) {
            // Doctor has not set availability for this date
            return Collections.emptyList();
        }

        // Step 2: Retrieve booked appointments for the doctor on the date
        List<Appointment> bookedAppointments = loadAppointmentsForDateAndDoctor(date, doctor);

        // Extract the booked TimeSlots
        List<TimeSlot> bookedTimeSlots = bookedAppointments.stream()
                .map(Appointment::getTimeSlot)
                .collect(Collectors.toList());

        // Step 3: Filter out the booked slots from the doctor's available slots
        List<TimeSlot> availableSlots = doctorAvailableSlots.stream()
                .filter(slot -> !bookedTimeSlots.contains(slot))
                .collect(Collectors.toList());

        return availableSlots;
    }

    private List<TimeSlot> generateAllTimeSlotsForDate(LocalDate date) {
        List<TimeSlot> timeSlots = new ArrayList<>();
        LocalTime startTime = LocalTime.of(9, 0); // Assuming appointments start at 9 AM
        LocalTime endTime = LocalTime.of(17, 0); // Assuming appointments end at 5 PM
        int slotDurationMinutes = 30; // Assuming each slot is 30 minutes

        while (startTime.isBefore(endTime)) {
            LocalDateTime slotStart = LocalDateTime.of(date, startTime);
            LocalDateTime slotEnd = slotStart.plusMinutes(slotDurationMinutes);
            timeSlots.add(new TimeSlot(slotStart, slotEnd, true));
            startTime = startTime.plusMinutes(slotDurationMinutes);
        }

        return timeSlots;
    }

    private List<Appointment> loadAppointmentsForDate(LocalDate date) {
        List<Appointment> appointmentsForDate = new ArrayList<>();
        try {
            List<String> lines = read("appts.txt");
            for (String line : lines) {
                Appointment appointment = deserializeAppointment(line);
                if (appointment.getTimeSlot().getStartTime().toLocalDate().equals(date)) {
                    appointmentsForDate.add(appointment);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return appointmentsForDate;
    }

    private List<Appointment> loadAppointmentsForDateAndDoctor(LocalDate date, Doctor doctor) {
        List<Appointment> appointmentsForDateAndDoctor = new ArrayList<>();
        try {
            List<String> lines = read("appts.txt");
            for (String line : lines) {
                Appointment appointment = deserializeAppointment(line);
                if (appointment.getDoctorId().equals(doctor.getHospitalID()) &&
                    appointment.getTimeSlot().getStartTime().toLocalDate().equals(date)) {
                    appointmentsForDateAndDoctor.add(appointment);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return appointmentsForDateAndDoctor;
    }

    // Appointment management methods
    public boolean addAppointment(Patient patient, Doctor doctor, LocalDate date, TimeSlot timeSlot) {
        // Check if the timeSlot is valid for the given date and doctor
        List<TimeSlot> availableSlots = getAvailableAppointmentSlots(date, doctor);
        if (!availableSlots.contains(timeSlot)) {
            System.out.println("The selected time slot is not available.");
            return false;
        }

        // Generate a new unique appointment ID
        int newAppointmentId = generateNewAppointmentId();

        // Create the new appointment
        Appointment newAppointment = new Appointment(newAppointmentId, 
                                                     patient.getHospitalID(), 
                                                     doctor.getHospitalID(), 
                                                     timeSlot, 
                                                     "Scheduled",
                                                     "Booked");

        // Add the new appointment to the list
        appointments.add(newAppointment);
        
        // Save the appointment to the file for persistence
        try {
            saveAppointmentsToFile("appts.txt");
        } catch (IOException e) {
            System.out.println("Failed to save the appointment to the file.");
            e.printStackTrace();
            return false;
        }

        System.out.println("Appointment successfully scheduled with Dr. " + doctor.getName() + " on " + date + " at " + timeSlot);
        return true;
    }

    public void removeAppointment(Appointment appointment) {
        appointments.remove(appointment);
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public void loadAppointmentsFromFile(String filename) throws IOException {
        List<String> stringArray = read(filename);
        appointments.clear();
        for (String line : stringArray) {
            appointments.add(deserializeAppointment(line));
        }
    }

    public static void saveAppointmentsToFile(String filename) throws IOException {
        List<String> stringList = new ArrayList<>();
        for (Appointment appointment : appointments) {
            stringList.add(serializeAppointment(appointment));
        }
        write(filename, stringList);
    }

    private static String serializeAppointment(Appointment appointment) {
        return String.join(SEPARATOR,
                String.valueOf(appointment.getId()),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                serializeTimeSlot(appointment.getTimeSlot()),
                appointment.getStatus(),
                appointment.getOutcomeRecord());
    }
    
    private static String serializeTimeSlot(TimeSlot timeSlot) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        return timeSlot.getStartTime().format(formatter) + "-" + timeSlot.getEndTime().format(formatter);
    }
    
    private Appointment deserializeAppointment(String appointmentData) {
        String[] fields = appointmentData.split("\\" + SEPARATOR);
        
        if (fields.length < 6) {
            System.err.println("Invalid appointment data: " + appointmentData);
            throw new IllegalArgumentException("Invalid appointment data: " + appointmentData);
        }
    
        int id = Integer.parseInt(fields[0]);
        String patientId = fields[1];
        String doctorId = fields[2];
        TimeSlot timeSlot = TimeSlot.parse(fields[3]);
        String status = fields[4];
        String outcomeRecord = fields[5];
    
        return new Appointment(id, patientId, doctorId, timeSlot, status, outcomeRecord);
    }

    // Additional Appointment Management Enhancements...

    // ====================== Doctor Assignment and Medical Records ========================= //

    
    public void addMedicalRecord(MedicalRecord record) throws IOException {
        medicalRecords.add(record);
        saveMedicalRecordsToFile("medical_records.txt");
    }
    
    private void loadMedicalRecordsFromFile(String filename) throws IOException {
        List<String> lines = read(filename);
        medicalRecords.clear();
        for (String line : lines) {
            medicalRecords.add(deserializeMedicalRecord(line));
        }
    }
    
    private void saveMedicalRecordsToFile(String filename) throws IOException {
        List<String> stringList = new ArrayList<>();
        for (MedicalRecord record : medicalRecords) {
            stringList.add(serializeMedicalRecord(record));
        }
        write(filename, stringList);
    }
    
    private String serializeMedicalRecord(MedicalRecord record) {
        // Using SEPARATOR to join fields
        StringBuilder sb = new StringBuilder();
        sb.append(record.getPatientID()).append(SEPARATOR);
        sb.append(record.getName()).append(SEPARATOR);
        sb.append(record.getDateOfBirth().format(DATE_FORMATTER)).append(SEPARATOR);
        sb.append(record.getGender()).append(SEPARATOR);
        sb.append(record.getContactInformation().getPhoneNumber()).append(SEPARATOR);
        sb.append(record.getContactInformation().getEmailAddress()).append(SEPARATOR);
        sb.append(record.getBloodType()).append(SEPARATOR);
    
        // Serialize Diagnoses
        if (record.getPastDiagnoses() != null && !record.getPastDiagnoses().isEmpty()) {
            String diagnoses = record.getPastDiagnoses().stream()
                .map(d -> d.getDescription() + ";" + d.getDate().format(DATE_FORMATTER))
                .collect(Collectors.joining(","));
            sb.append(diagnoses).append(SEPARATOR);
        } else {
            sb.append("NULL").append(SEPARATOR);
        }
    
        // Serialize Treatments
        if (record.getPastTreatments() != null && !record.getPastTreatments().isEmpty()) {
            String treatments = record.getPastTreatments().stream()
                .map(Treatment::serialize)
                .collect(Collectors.joining("|"));
            sb.append(treatments).append(SEPARATOR);
        } else {
            sb.append("NULL").append(SEPARATOR);
        }
    
        // Serialize Assigned Doctor ID
        sb.append(record.getAssignedDoctorId() != null ? record.getAssignedDoctorId() : "NULL");
    
        return sb.toString();
    }

    
    
    private MedicalRecord deserializeMedicalRecord(String data) {
        String[] fields = data.split("\\" + SEPARATOR);
        if (fields.length < 9) { // Updated to expect 9 fields
            throw new IllegalArgumentException("Invalid medical record data: " + data);
        }
    
        String patientID = fields[0];
        String name = fields[1];
        LocalDate dob = LocalDate.parse(fields[2], DATE_FORMATTER);
        String gender = fields[3];
        String phone = fields[4];
        String email = fields[5];
        String bloodType = fields[6];
    
        ContactInformation contactInfo = new ContactInformation(phone, email);
    
        List<Diagnosis> diagnoses = new ArrayList<>();
        if (!fields[7].equals("NULL")) {
            String[] diagParts = fields[7].split(",");
            for (String diag : diagParts) {
                String[] diagFields = diag.split(";");
                if (diagFields.length == 2) {
                    diagnoses.add(new Diagnosis(diagFields[0], LocalDate.parse(diagFields[1], DATE_FORMATTER)));
                }
            }
        }
    
        List<Treatment> treatments = new ArrayList<>();
        if (!fields[8].equals("NULL")) {
            String[] treatParts = fields[8].split("\\|");
            for (String treat : treatParts) {
                treatments.add(Treatment.deserialize(treat));
            }
        }
    
        // Assigned Doctor ID
        String assignedDoctorId = fields.length > 9 && !fields[9].equals("NULL") ? fields[9] : null;
    
        MedicalRecord record = new MedicalRecord(patientID, name, dob, gender, contactInfo, bloodType, diagnoses, treatments);
        record.setAssignedDoctorId(assignedDoctorId);
    
        return record;
    }

    /**
     * Assigns a doctor to a patient by updating the patient's medical record.
     *
     * @param doctorId  The hospital ID of the doctor.
     * @param patientId The hospital ID of the patient.
     * @throws IOException If an I/O error occurs during the update.
     */
    public void assignDoctorToPatient(String doctorId, String patientId) throws IOException {
        MedicalRecord record = getMedicalRecordByPatientId(patientId);
        if (record == null) {
            System.err.println("Medical record for patient ID " + patientId + " not found.");
            return;
        }

        if (record.getAssignedDoctorId() != null) {
            System.out.println("Patient already has a doctor assigned: " + record.getAssignedDoctorId());
            return;
        }

        // Assign the doctor
        record.setAssignedDoctorId(doctorId);
        updateMedicalRecord(record);
        System.out.println("Doctor " + doctorId + " has been assigned to patient " + patientId + ".");
    }

    /**
     * Unassigns a doctor from a patient by updating the patient's medical record.
     *
     * @param doctorId  The hospital ID of the doctor.
     * @param patientId The hospital ID of the patient.
     * @throws IOException If an I/O error occurs during the update.
     */
    public void unassignDoctorFromPatient(String doctorId, String patientId) throws IOException {
        MedicalRecord record = getMedicalRecordByPatientId(patientId);
        if (record == null) {
            System.err.println("Medical record for patient ID " + patientId + " not found.");
            return;
        }

        if (record.getAssignedDoctorId() == null) {
            System.out.println("No doctor is currently assigned to patient " + patientId + ".");
            return;
        }

        if (!record.getAssignedDoctorId().equals(doctorId)) {
            System.out.println("Doctor " + doctorId + " is not assigned to patient " + patientId + ".");
            return;
        }

        // Unassign the doctor
        record.setAssignedDoctorId(null);
        updateMedicalRecord(record);
        System.out.println("Doctor " + doctorId + " has been unassigned from patient " + patientId + ".");
    }

    /**
     * Retrieves the MedicalRecord object for a given patient ID.
     *
     * @param patientId The hospital ID of the patient.
     * @return The MedicalRecord object, or null if not found.
     */
    public MedicalRecord getMedicalRecordByPatientId(String patientId) {
        for (MedicalRecord record : medicalRecords) {
            if (record.getPatientID().equals(patientId)) {
                return record;
            }
        }
        return null;
    }

    /**
     * Updates an existing MedicalRecord in the list and persists the changes.
     *
     * @param updatedRecord The updated MedicalRecord object.
     * @throws IOException If an I/O error occurs during saving.
     */
    public void updateMedicalRecord(MedicalRecord updatedRecord) throws IOException {
        boolean found = false;
        for (int i = 0; i < medicalRecords.size(); i++) {
            if (medicalRecords.get(i).getPatientID().equals(updatedRecord.getPatientID())) {
                medicalRecords.set(i, updatedRecord);
                found = true;
                break;
            }
        }

        if (found) {
            saveMedicalRecordsToFile("medical_records.txt");
        } else {
            System.err.println("Medical record for patient ID " + updatedRecord.getPatientID() + " not found.");
        }
    }
    

    // Additional Appointment Management Enhancements
    public List<Appointment> getAppointmentsByDoctorId(String doctorId) {
        return appointments.stream()
                .filter(appt -> appt.getDoctorId().equals(doctorId))
                .collect(Collectors.toList());
    }
    
    public List<Appointment> getPendingAppointmentsByDoctorId(String doctorId) {
        return appointments.stream()
                .filter(appt -> appt.getDoctorId().equals(doctorId) && appt.getStatus().equalsIgnoreCase("Pending"))
                .collect(Collectors.toList());
    }
    
    public List<Appointment> getUpcomingAppointmentsByDoctorId(String doctorId) {
        LocalDateTime now = LocalDateTime.now();
        return appointments.stream()
                .filter(appt -> appt.getDoctorId().equals(doctorId) &&
                                appt.getTimeSlot().getStartTime().isAfter(now) &&
                                !appt.getStatus().equalsIgnoreCase("Declined"))
                .collect(Collectors.toList());
    }
    
    public Appointment getAppointmentById(int appointmentId) {
        for (Appointment appt : appointments) {
            if (appt.getId() == appointmentId) {
                return appt;
            }
        }
        return null;
    }
    
    public void updateAppointment(Appointment updatedAppt) throws IOException {
        for (int i = 0; i < appointments.size(); i++) {
            if (appointments.get(i).getId() == updatedAppt.getId()) {
                appointments.set(i, updatedAppt);
                saveAppointmentsToFile("appts.txt");
                break;
            }
        }
    }
}