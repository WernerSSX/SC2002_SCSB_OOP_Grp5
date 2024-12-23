package menus;
import HospitalNotificationSystem.NotifyDoctor;
import db.TextDB;
import items.appointments.Appointment;
import items.appointments.TimeSlot;
import items.medical_records.MedicalRecord;
import items.medical_records.Treatment;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import user_classes.Doctor;
import user_classes.Patient;

/**
 * PatientMenu
 * Displays a menu for patients to view and manage their appointments, medical records, and contact information.
 */
public final class PatientMenu {
    private TextDB textDB;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Constructor to initialize PatientMenu with a TextDB instance.
     * @param textDB A TextDB instance for data retrieval and storage.
     */
    public PatientMenu(TextDB textDB) {
        this.textDB = textDB;
    }

    /**
     * Displays the patient menu and processes user input.
     * @param scanner Scanner object to take user input.
     * @param patient The patient using the menu.
     * @throws IOException If an input or output exception occurs.
     */
    public void showMenu(Scanner scanner, Patient patient) throws IOException {
        while (true) {
            System.out.println("\nPatient Menu:");
            System.out.println("1. View Medical Record");
            System.out.println("2. Update Contact Information");
            System.out.println("3. View Available Appointment Slots");
            System.out.println("4. Schedule Appointment");
            System.out.println("5. Reschedule Appointment");
            System.out.println("6. Cancel Appointment");
            System.out.println("7. View Appointment Status");
            System.out.println("8. View Appointment Outcome Records");
            System.out.println("9. Change Password");
            System.out.println("0. Log out");
            System.out.print("Enter your choice: ");
            int choice = getIntInput(scanner);

            switch (choice) {
                case 1:
                    viewMedicalRecord(patient);
                    break;
                case 2:
                    updateContactInformation(scanner, patient);
                    break;
                case 3:
                    viewAvailableAppointmentSlotsWithDoctor(scanner);
                    break;
                case 4:
                    scheduleAppointment(scanner, patient);
                    break;
                case 5:
                    rescheduleAppointment(scanner, patient);
                    break;
                case 6:
                    cancelAppointment(scanner, patient);
                    break;
                case 7:
                    viewAppointmentStatus(patient);
                    break;
                case 8:
                    viewPastAppointmentOutcomeRecords(patient);
                    break;
                case 9:
                    changePassword(scanner, patient);
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    
    /**
     * Displays a patient's past appointment outcome records.
     * @param patient The patient whose records are being accessed.
     */
    private void viewPastAppointmentOutcomeRecords(Patient patient) {
    	MedicalRecord record = textDB.getMedicalRecordByPatientId(patient.getHospitalID());
    	if (record == null) {
    		System.out.println("No appointment recorded.");
    		return;
    	}
    	System.out.println();
    	List<Treatment> pastTreatments = record.getPastTreatments();
    	
        System.out.println("Appointment records:");
        if (pastTreatments == null || pastTreatments.isEmpty()) {
            System.out.println("  - No appointment recorded.");
        } else {
            for (Treatment treat : pastTreatments) {
                treat.display();
            }
        }
        System.out.println();	
    }
    
    /**
     * Displays a patient's medical record.
     * @param patient The patient whose medical record is being viewed.
     */
    private void viewMedicalRecord(Patient patient) {
        MedicalRecord record = textDB.getMedicalRecordByPatientId(patient.getHospitalID());
        if (record == null) {
            System.out.println("No medical record found.");
            return;
        }
        System.out.println();
        record.display();
    }

    /**
     * Updates the contact information of the patient.
     * @param scanner Scanner object for user input.
     * @param patient The patient whose contact information is updated.
     */
    private void updateContactInformation(Scanner scanner, Patient patient) {
        System.out.print("Enter new email address: ");
        String email = scanner.nextLine();
        System.out.print("Enter new phone number: ");
        String phone = scanner.nextLine();
        
        MedicalRecord medicalRecord = patient.getMedicalRecord();
        if (medicalRecord != null) {
            medicalRecord.getContactInformation().setEmailAddress(email);
            medicalRecord.getContactInformation().setPhoneNumber(phone);
            try {
                textDB.updateMedicalRecord(medicalRecord);
                System.out.println("Contact information updated successfully.");
            } catch (IOException e) {
                System.out.println("Failed to update contact information.");
                e.printStackTrace();
            }
        } else {
            System.out.println("Medical record not found. Cannot update contact information.");
        }
    }
    
    /**
     * Displays the current appointment status for the patient.
     * @param patient The patient whose appointments are being viewed.
     */
    private void viewAppointmentStatus(Patient patient) {
        List<Appointment> appointments = textDB.getAppointments().stream()
            .filter(appt -> appt.getPatientId().equals(patient.getHospitalID()))
            .collect(Collectors.toList());

        if (appointments.isEmpty()) {
            System.out.println("You have no appointments.");
            return;
        }

        System.out.println("\nYour Appointments:");
        for (Appointment appointment : appointments) {
            Doctor doctor = (Doctor) textDB.getUserByHospitalID(appointment.getDoctorId());
            String doctorName = (doctor != null) ? doctor.getName() : "Unknown Doctor";

            // Extract date and time from TimeSlot
            LocalDate appointmentDate = appointment.getTimeSlot().getStartTime().toLocalDate();
            String formattedDate = appointmentDate.format(DATE_FORMATTER);
            String formattedStartTime = appointment.getTimeSlot().getStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            String formattedEndTime = appointment.getTimeSlot().getEndTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));

            System.out.println("Appointment ID: " + appointment.getId() +
                            ", Doctor: Dr. " + doctorName +
                            ", Date: " + formattedDate +
                            ", Time: " + formattedStartTime + " - " + formattedEndTime +
                            ", Status: " + appointment.getStatus());
        }
    }

    /**
     * Displays a list of available appointment slots with a specific doctor.
     * @param scanner Scanner object for user input.
     */
    private void viewAvailableAppointmentSlotsWithDoctor(Scanner scanner) {
        // Step 1: Display list of available doctors
        List<Doctor> doctors = textDB.getAllDoctors();
        if (doctors.isEmpty()) {
            System.out.println("No doctors are currently available.");
            return;
        }

        System.out.println("\nAvailable Doctors:");
        for (int i = 0; i < doctors.size(); i++) {
            System.out.println((i + 1) + ". Dr. " + doctors.get(i).getName() + " (ID: " + doctors.get(i).getHospitalID() + ")");
        }

        System.out.print("Enter the number corresponding to the doctor you want to view available slots for: ");
        int doctorIndex = getIntInput(scanner) - 1;

        if (doctorIndex < 0 || doctorIndex >= doctors.size()) {
            System.out.println("Invalid selection. Please try again.");
            return;
        }

        Doctor selectedDoctor = doctors.get(doctorIndex);
        System.out.println("You have selected Dr. " + selectedDoctor.getName());

        // Step 2: Call the updated viewAvailableAppointmentSlots method
        viewAvailableAppointmentSlots(scanner, selectedDoctor);
    }

    /**
     * Displays available appointment slots for a specific doctor on a given date.
     * @param scanner Scanner object for user input.
     * @param doctor The doctor for whom to view available slots.
     */
    private void viewAvailableAppointmentSlots(Scanner scanner, Doctor doctor) {
        System.out.print("Enter the date (yyyy-MM-dd) for which you want to see available slots: ");
        String dateInput = scanner.nextLine();
        LocalDate date;

        try {
            date = LocalDate.parse(dateInput, DATE_FORMATTER);
        } catch (Exception e) {
            System.out.println("Invalid date format. Please try again.");
            return;
        }

        List<TimeSlot> availableSlots = textDB.getAvailableAppointmentSlots(date, doctor);

        if (availableSlots.isEmpty()) {
            // Check if the doctor has set availability for this date
            List<TimeSlot> doctorAvailableSlots = doctor.getAvailableTimeSlots(date);
            if (doctorAvailableSlots.isEmpty()) {
                System.out.println("Dr. " + doctor.getName() + " has not set availability for " + date + ".");
            } else {
                System.out.println("No available slots for Dr. " + doctor.getName() + " on " + date + ".");
            }
        } else {
            System.out.println("\nAvailable Appointment Slots with Dr. " + doctor.getName() + " on " + date + ":");
            for (int i = 0; i < availableSlots.size(); i++) {
                TimeSlot slot = availableSlots.get(i);
                System.out.println((i + 1) + ". " + slot.getStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) +
                        " - " + slot.getEndTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
        }
    }

    /**
     * Schedules a new appointment for the patient.
     * @param scanner Scanner object for user input.
     * @param patient The patient scheduling the appointment.
     */
    private void scheduleAppointment(Scanner scanner, Patient patient) {
        // Step 1: Ask for the date of the appointment
        System.out.print("Enter the date (yyyy-MM-dd) for which you want to schedule an appointment: ");
        String dateInput = scanner.nextLine();
        LocalDate date;
        
        try {
            date = LocalDate.parse(dateInput, DATE_FORMATTER);
        } catch (Exception e) {
            System.out.println("Invalid date format. Please try again.");
            return;
        }

        // Step 2: Display list of available doctors
        List<Doctor> doctors = textDB.getAllDoctors();
        if (doctors.isEmpty()) {
            System.out.println("No doctors are currently available.");
            return;
        }

        System.out.println("\nAvailable Doctors:");
        for (int i = 0; i < doctors.size(); i++) {
            System.out.println((i + 1) + ". Dr. " + doctors.get(i).getName() + " (ID: " + doctors.get(i).getHospitalID() + ")");
        }

        System.out.print("Enter the number corresponding to the doctor you want to book an appointment with: ");
        int doctorIndex = getIntInput(scanner) - 1;

        if (doctorIndex < 0 || doctorIndex >= doctors.size()) {
            System.out.println("Invalid selection. Please try again.");
            return;
        }

        Doctor selectedDoctor = doctors.get(doctorIndex);
        System.out.println("You have selected Dr. " + selectedDoctor.getName());

        // Step 3: Display available time slots for the selected doctor and date
        List<TimeSlot> availableSlots = textDB.getAvailableAppointmentSlots(date, selectedDoctor);
        
        if (availableSlots.isEmpty()) {
            System.out.println("No available slots for Dr. " + selectedDoctor.getName() + " on " + date + ". Please choose a different date or doctor.");
            return;
        }

        System.out.println("\nAvailable Appointment Slots with Dr. " + selectedDoctor.getName() + " on " + date + ":");
        for (int i = 0; i < availableSlots.size(); i++) {
            System.out.println((i + 1) + ". " + availableSlots.get(i));
        }

        // Step 4: Ask the user to select a time slot by entering its index
        System.out.print("Enter the number corresponding to the time slot you want to book: ");
        int slotIndex = getIntInput(scanner) - 1;

        if (slotIndex < 0 || slotIndex >= availableSlots.size()) {
            System.out.println("Invalid selection. Please try again.");
            return;
        }

        TimeSlot selectedSlot = availableSlots.get(slotIndex);
        System.out.println("You have selected " + selectedSlot);

        // Step 5: Use the updated addAppointment method in TextDB
        boolean success = textDB.addAppointment(patient, selectedDoctor, date, selectedSlot);

        if (!success) {
            System.out.println("Failed to submit appointment request. Please try again.");
        } else {
            // Notify doctor when appointment is added
            NotifyDoctor.getInstance().notifyDoctorUser("New Appointment request from " + patient + " on " + dateInput);
        }
    }

    /**
     * Reschedules an existing appointment for a patient.
     * 
     * This method allows a patient to view their current appointments, select an appointment to reschedule, 
     * choose a new date, and select a new time slot and/or doctor. 
     * It then updates the appointment details in the database if the rescheduling is confirmed.
     * 
     * @param scanner The scanner object to read user input.
     * @param patient The patient whose appointment is being rescheduled.
     */
    private void rescheduleAppointment(Scanner scanner, Patient patient) {
        // Step 1: Retrieve and display the patient's appointments
        List<Appointment> patientAppointments = textDB.getAppointments().stream()
            .filter(appt -> appt.getPatientId().equals(patient.getHospitalID()))
            .collect(Collectors.toList());
    
        if (patientAppointments.isEmpty()) {
            System.out.println("You have no appointments to reschedule.");
            return;
        }
    
        System.out.println("\nYour Appointments:");
        for (Appointment appointment : patientAppointments) {
            LocalDate appointmentDate = appointment.getTimeSlot().getStartTime().toLocalDate();
            String formattedDate = appointmentDate.format(DATE_FORMATTER);
            String formattedStartTime = appointment.getTimeSlot().getStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            String formattedEndTime = appointment.getTimeSlot().getEndTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));

            System.out.println("Appointment ID: " + appointment.getId() +
                            ", Doctor: " + textDB.getUserByHospitalID(appointment.getDoctorId()).getName() +
                            ", Date: " + formattedDate +
                            ", Time: " + formattedStartTime + " - " + formattedEndTime +
                            ", Status: " + appointment.getStatus());

        }
    
        // Step 2: Prompt user to enter the Appointment ID to reschedule
        System.out.print("\nEnter the ID of the appointment you want to reschedule: ");
        int appointmentId = getIntInput(scanner);
    
        // Step 3: Verify if the appointment exists and belongs to the patient
        Appointment appointmentToReschedule = textDB.getAppointmentById(appointmentId);
        if (appointmentToReschedule == null || !appointmentToReschedule.getPatientId().equals(patient.getHospitalID())) {
            System.out.println("No such appointment found. Please check the Appointment ID and try again.");
            return;
        }
    
        // Step 4: Choose a new date
        System.out.print("Enter the new date for the appointment (yyyy-MM-dd): ");
        String newDateInput = scanner.nextLine();
        LocalDate newDate;
        try {
            newDate = LocalDate.parse(newDateInput, DATE_FORMATTER);
        } catch (Exception e) {
            System.out.println("Invalid date format. Please try again.");
            return;
        }

        if (newDate.isBefore(LocalDate.now())) {
            System.out.println("Cannot reschedule to a past date. Please choose a future date.");
            return;
        }
    
        // Step 5: Choose a doctor (optional: keep the same doctor)
        System.out.print("Do you want to keep the same doctor? (yes/no): ");
        String keepDoctor = scanner.nextLine().trim().toLowerCase();
    
        Doctor selectedDoctor;
        if (keepDoctor.equals("yes")) {
            selectedDoctor = (Doctor) textDB.getUserByHospitalID(appointmentToReschedule.getDoctorId());
            System.out.println("You have chosen to keep Dr. " + selectedDoctor.getName());
        } else if (keepDoctor.equals("no")) {
            // Display list of available doctors
            List<Doctor> doctors = textDB.getAllDoctors();
            if (doctors.isEmpty()) {
                System.out.println("No doctors are currently available.");
                return;
            }
    
            System.out.println("\nAvailable Doctors:");
            for (int i = 0; i < doctors.size(); i++) {
                System.out.println((i + 1) + ". Dr. " + doctors.get(i).getName() + " (ID: " + doctors.get(i).getHospitalID() + ")");
            }
    
            System.out.print("Enter the number corresponding to the doctor you want to book an appointment with: ");
            int doctorIndex = getIntInput(scanner) - 1;
    
            if (doctorIndex < 0 || doctorIndex >= doctors.size()) {
                System.out.println("Invalid selection. Please try again.");
                return;
            }
    
            selectedDoctor = doctors.get(doctorIndex);
            System.out.println("You have selected Dr. " + selectedDoctor.getName());
        } else {
            System.out.println("Invalid input. Please enter 'yes' or 'no'.");
            return;
        }
    
        // Step 6: Display available time slots for the selected doctor and new date
        List<TimeSlot> availableSlots = textDB.getAvailableAppointmentSlots(newDate, selectedDoctor);
    
        if (availableSlots.isEmpty()) {
            // Check if the doctor has set availability for this date
            List<TimeSlot> doctorAvailableSlots = selectedDoctor.getAvailableTimeSlots(newDate);
            if (doctorAvailableSlots.isEmpty()) {
                System.out.println("Dr. " + selectedDoctor.getName() + " has not set availability for " + newDate + ".");
            } else {
                System.out.println("No available slots for Dr. " + selectedDoctor.getName() + " on " + newDate + ".");
            }
            return;
        }
    
        System.out.println("\nAvailable Appointment Slots with Dr. " + selectedDoctor.getName() + " on " + newDate + ":");
        for (int i = 0; i < availableSlots.size(); i++) {
            TimeSlot slot = availableSlots.get(i);
            System.out.println((i + 1) + ". " + slot.getStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) +
                               " - " + slot.getEndTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    
        // Step 7: Prompt user to select a new time slot
        System.out.print("Enter the number corresponding to the new time slot you want to book: ");
        int slotIndex = getIntInput(scanner) - 1;
    
        if (slotIndex < 0 || slotIndex >= availableSlots.size()) {
            System.out.println("Invalid selection. Please try again.");
            return;
        }
    
        TimeSlot selectedSlot = availableSlots.get(slotIndex);
        System.out.println("You have selected " + selectedSlot);
    
        // Step 8: Confirm rescheduling
        System.out.print("Are you sure you want to reschedule this appointment? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        if (!confirmation.equals("yes")) {
            System.out.println("Appointment rescheduling aborted.");
            return;
        }
    
        // Step 9: Proceed with rescheduling
        try {
            // Create a new TimeSlot object (assuming TimeSlot is mutable)
            TimeSlot newTimeSlot = new TimeSlot(selectedSlot.getStartTime(), selectedSlot.getEndTime(), false);
    
            // Update the appointment details
            appointmentToReschedule.setDoctorId(selectedDoctor.getHospitalID());
            appointmentToReschedule.setTimeSlot(newTimeSlot);
            appointmentToReschedule.setStatus("Rescheduled");
    
            // Update the appointment in TextDB
            textDB.updateAppointment(appointmentToReschedule);
    
            System.out.println("Appointment rescheduled successfully to " + newDate + " at " + newTimeSlot + ".");
            NotifyDoctor.getInstance().notifyDoctorUser("Appointment rescheduled from " + patient + " on " + newDate + "at" + newTimeSlot);
        } catch (IOException e) {
            System.out.println("Failed to reschedule appointment due to an internal error. Please try again later.");
            e.printStackTrace();
        }
    }
    

    /**
     * Cancels an existing appointment for a patient.
     * 
     * This method allows a patient to view their current appointments, select an appointment to cancel, 
     * and confirm the cancellation. The appointment is then removed from the system.
     * 
     * @param scanner The scanner object to read user input.
     * @param patient The patient whose appointment is being canceled.
     */
    private void cancelAppointment(Scanner scanner, Patient patient) {
        // Step 1: Retrieve and display the patient's appointments
        List<Appointment> patientAppointments = textDB.getAppointments().stream()
            .filter(appt -> appt.getPatientId().equals(patient.getHospitalID()))
            .collect(Collectors.toList());
    
        if (patientAppointments.isEmpty()) {
            System.out.println("You have no appointments to cancel.");
            return;
        }
    
        System.out.println("\nYour Appointments:");
        for (Appointment appointment : patientAppointments) {
            LocalDate appointmentDate = appointment.getTimeSlot().getStartTime().toLocalDate();
            String formattedDate = appointmentDate.format(DATE_FORMATTER);
            String formattedStartTime = appointment.getTimeSlot().getStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            String formattedEndTime = appointment.getTimeSlot().getEndTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));

            System.out.println("Appointment ID: " + appointment.getId() +
                            ", Doctor: " + textDB.getUserByHospitalID(appointment.getDoctorId()).getName() +
                            ", Date: " + formattedDate +
                            ", Time: " + formattedStartTime + " - " + formattedEndTime +
                            ", Status: " + appointment.getStatus());

        }
    
        // Step 2: Prompt user to enter the Appointment ID to cancel
        System.out.print("\nEnter the ID of the appointment you want to cancel: ");
        int appointmentId = getIntInput(scanner);
    
        // Step 3: Verify if the appointment exists and belongs to the patient
        Appointment appointmentToCancel = textDB.getAppointmentById(appointmentId);
        if (appointmentToCancel == null || !appointmentToCancel.getPatientId().equals(patient.getHospitalID())) {
            System.out.println("No such appointment found. Please check the Appointment ID and try again.");
            return;
        }
    
        // Step 4: Confirm cancellation with the user
        System.out.print("Are you sure you want to cancel this appointment? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        if (!confirmation.equals("yes")) {
            System.out.println("Appointment cancellation aborted.");
            return;
        }
    
        // Step 5: Proceed with cancellation
        boolean success = textDB.cancelAppointment(patient, appointmentId);
        if (success) {
            System.out.println("Appointment canceled successfully.");
            NotifyDoctor.getInstance().notifyDoctorUser("Appointment >> " + appointmentToCancel.toString() + " has been cancelled.");
        } else {
            System.out.println("Failed to cancel appointment. Please try again.");
        }
    }
    
    private static String hashPassword(String hospitalID, String password) {
        String combined = hospitalID + password;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }
    /**
     * Changes the password of a patient.
     * 
     * This method allows a patient to change their password by entering the current password, 
     * then the new password. It checks if the current password is correct before updating it.
     * 
     * @param scanner The scanner object to read user input.
     * @param patient The patient whose password is being changed.
     */
    private void changePassword(Scanner scanner, Patient patient) {
        System.out.print("Enter current password: ");
        String currentPassword = scanner.nextLine();
        System.out.print("Enter new password: ");
        String newPassword = scanner.nextLine();
        
        // Assuming Patient class has a method to verify the current password
        if (patient.getPassword().equals(hashPassword(patient.getHospitalID(), currentPassword))) {
            try {
                patient.setPassword(hashPassword(patient.getHospitalID(), newPassword));
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Password changed successfully.");
        } else {
            System.out.println("Failed to change password. Please try again.");
        }
    }

    /**
     * Helper method to get an integer input from the user.
     * 
     * This method ensures that the user enters a valid integer.
     * 
     * @param scanner The scanner object to read user input.
     * @return The integer input entered by the user.
     */
    private int getIntInput(Scanner scanner) {
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid input. Please enter a number: ");
            scanner.next();
        }
        int out = scanner.nextInt();
        scanner.nextLine();
        return out;
    }
}
