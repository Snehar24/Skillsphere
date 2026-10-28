# 🎓 SkillSphere — E-Learning Platform

**SkillSphere** is a full-featured Enterprise Learning and Assessment Management System built with **Spring Boot**. It provides a comprehensive platform for managing courses, batches, students, trainers, assignments, exams, certificates, and payments — all through role-based dashboards.

---

## ✨ Features

### 👨‍💼 Admin Panel
- **Dashboard** — Overview of platform metrics and activity
- **User Management** — Manage students, trainers, and admin users
- **Course & Batch Management** — Create courses, organize batches, and handle enrollments
- **Assignment & Exam Management** — Create, assign, and track assignments and exams
- **Attendance Tracking** — Bulk attendance marking and reports
- **Certificate Generation** — Issue and manage course completion certificates
- **Payment Management** — Track and manage student payments
- **Project Management** — Assign and evaluate student projects
- **Announcements & Notifications** — Broadcast announcements to users
- **Reports & Audit Logs** — Generate performance reports and view system audit trails

### 👨‍🏫 Trainer Portal
- Manage assigned batches and enrolled students
- Create and evaluate assignments, exams, and projects
- Mark and review attendance
- View evaluation dashboards

### 👨‍🎓 Student Portal
- View enrolled courses and batch details
- Submit assignments and projects
- Take exams online
- Track attendance and view certificates
- Make payments with checkout flow
- Receive notifications and announcements

### 🔐 Authentication & Security
- JWT-based authentication
- Role-based access control (Admin, Trainer, Student)
- Password reset via email
- Spring Security integration

### 📜 Certificate Verification
- Public certificate verification page
- Unique certificate validation

---

## 🛠️ Tech Stack

| Layer          | Technology                          |
|----------------|-------------------------------------|
| **Backend**    | Java 17, Spring Boot 3.3.4          |
| **Security**   | Spring Security, JWT (jjwt 0.11.5)  |
| **Database**   | MySQL with Spring Data JPA          |
| **Frontend**   | Thymeleaf, HTML, CSS, JavaScript    |
| **Email**      | Spring Boot Mail (SMTP)             |
| **Documents**  | Apache PDFBox, Apache POI           |
| **Build**      | Maven                               |
| **Others**     | Lombok, Bean Validation             |

---

## 📁 Project Structure

```
SkillSphere/
├── src/main/java/com/skillsphere/
│   ├── admin/           # Admin controllers & DTOs
│   ├── assignment/      # Assignment CRUD & submissions
│   ├── attendance/      # Attendance tracking
│   ├── auth/            # Authentication & password reset
│   ├── batch/           # Batch & enrollment management
│   ├── certificate/     # Certificate generation & verification
│   ├── common/          # Shared configs, enums & audit logging
│   ├── course/          # Course management
│   ├── dashboard/       # Dashboard services
│   ├── evaluation/      # Evaluation management
│   ├── exam/            # Exam creation, attempts & results
│   ├── exception/       # Global exception handling
│   ├── notification/    # Notifications & announcements
│   ├── payment/         # Payment processing
│   ├── project/         # Project assignments & submissions
│   ├── report/          # Reports & analytics
│   ├── security/        # JWT filter, config & user details
│   ├── student/         # Student CRUD
│   ├── trainer/         # Trainer CRUD
│   └── user/            # Base user entity & repository
├── src/main/resources/
│   ├── templates/       # Thymeleaf HTML templates
│   │   ├── admin/       # 18 admin pages
│   │   ├── student/     # 8 student pages
│   │   └── trainer/     # 7 trainer pages
│   ├── static/          # Static assets
│   └── application.properties
└── pom.xml
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **MySQL 8.0+**

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/ITMohanraj/SkillSphere.git
   cd SkillSphere
   ```

2. **Create the MySQL database**
   ```sql
   CREATE DATABASE skillsphere1;
   ```

3. **Configure environment variables** (or update `application.properties`)
   ```bash
   export DB_USERNAME=root
   export DB_PASSWORD=your_password
   export JWT_SECRET=your_jwt_secret_key
   export MAIL_USERNAME=your_email@gmail.com
   export MAIL_PASSWORD=your_app_password
   ```

4. **Build and run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Access the application**
   ```
   http://localhost:8080
   ```

---

## 🔑 Default Roles

| Role       | Access Level                                      |
|------------|---------------------------------------------------|
| **ADMIN**  | Full platform management                          |
| **TRAINER**| Batch, assignment, exam, and attendance management |
| **STUDENT**| Course access, submissions, and payments           |

---
