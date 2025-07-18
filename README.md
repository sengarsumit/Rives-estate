Real Estate Rental Backend API
Welcome to the Real Estate Rental Platform Backend — a robust Spring Boot API designed for property listings, secure user access, and seamless image management. This backend is the foundation for modern real estate rental apps, supporting both dealers (property managers) and renters, with features inspired by real-world scalability and security needs.

🚀 Key Features
🔐 Authentication & Authorization
Secure JWT-based login and signup

Role-based access: Supports DEALER and USER distinctions

All critical routes are protected; only authorized users perform sensitive operations

🏠 Property Management
Dealers can:

Create new property listings

Upload multiple images (securely stored on Cloudinary)

Update or delete only their own properties

Users can:

Browse property listings

(Planned) Filter properties by locality or rent

🖼️ Image Handling
Multiple property images supported via multipart/form-data

Images safely stored on Cloudinary CDN

All image metadata (URL, public_id) tracked in the dedicated PropertyImage table

🗄️ Tech Stack Overview
Technology	Purpose
Spring Boot	Core backend framework
Spring Security + JWT	User authentication & authorization
MySQL	Relational data storage
Hibernate (JPA)	Object–Relational Mapping (ORM)
Cloudinary	Image storage and delivery
Lombok	Reduces Java boilerplate code
Maven	Dependency management & builds


🛠️ Getting Started

Prerequisites
Java 17+

MySQL 8+

Maven

Cloudinary account

1. Clone this Repo
bash
git clone https://github.com/sengarsumit/Rives-estate.git

2. Configure Your Environment
Create application.properties under src/main/resources:

text
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/realestate
spring.datasource.username=your_username
spring.datasource.password=your_password

# JWT Secret
jwt.secret=your_jwt_secret

# Cloudinary
cloudinary.cloud_name=your_cloud_name
cloudinary.api_key=your_api_key
cloudinary.api_secret=your_api_secret
3. Run the Project
bash
mvn clean install
mvn spring-boot:run
🔑 API Endpoints
Auth
Method	Endpoint	Description
POST	/api/auth/signup	Register new user or dealer
POST	/api/auth/login	Get JWT with credentials
Properties
Method	Endpoint	Description
POST	/api/properties	Dealer creates a property listing
POST	/api/properties/{id}/images	Upload multiple images to the property
GET	/api/properties	View all properties
PUT	/api/properties/{id}	Dealer updates property
DELETE	/api/properties/{id}	Dealer deletes property
🖼️ Image Upload (Sample)
Form type: multipart/form-data

Key: images

Value: Upload multiple image files

🗃️ Database Schema (Simplified)

User: id, email, username, password, role

Property: id, title, description, rental, dealer (owner)

PropertyImage: id, property_id (FK), image_url, public_id (Cloudinary)

<img width="876" height="1904" alt="Rives-estate" src="https://github.com/user-attachments/assets/bc07885c-8424-43e7-a5f8-5939823681fd" />


🧪 Testing
Easily test endpoints using Postman or cURL.
A sample Postman collection will be added soon!

📦 Future Enhancements
Advanced search & filter: by locality, rent range

Booking & reservation system for renters

Email notifications

Admin dashboard for platform management

🙌 Contribution
Open to all contributions — fork, create issues, or submit pull requests!


✉️ Contact
Sumit Sengar
📍 Bangalore, India
