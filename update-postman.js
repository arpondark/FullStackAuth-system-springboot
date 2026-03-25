const fs = require('fs');

const file = 'Auth rest apis.postman_collection.json';
const data = JSON.parse(fs.readFileSync(file, 'utf8'));

const adminFolder = {
    "name": "Admin",
    "item": [
        {
            "name": "Create Admin or User",
            "request": {
                "auth": {
                    "type": "bearer",
                    "bearer": [
                        {
                            "key": "token",
                            "value": "{{jwt_token}}",
                            "type": "string"
                        }
                    ]
                },
                "method": "POST",
                "header": [
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\\n    \\\"name\\\": \\\"New User\\\",\\n    \\\"email\\\": \\\"newuser@example.com\\\",\\n    \\\"password\\\": \\\"securepassword\\\",\\n    \\\"role\\\": \\\"ADMIN\\\"\\n}",
                    "options": {
                        "raw": {
                            "language": "json"
                        }
                    }
                },
                "url": {
                    "raw": "{{base_url}}/api/v1/admin/users",
                    "host": [
                        "{{base_url}}"
                    ],
                    "path": [
                        "api",
                        "v1",
                        "admin",
                        "users"
                    ]
                },
                "description": "Create a new user or admin account.\\n\\n**Headers:**\\n- `Authorization: Bearer <token>` (must have ADMIN role)\\n\\n**Request Body:**\\n- `name` (string, required): User's name\\n- `email` (string, required): User's email\\n- `password` (string, required): User's password (min 6 chars)\\n- `role` (string, required): \\\"ADMIN\\\" or \\\"USER\\\"\\n\\n**Response:**\\n```json\\n{\\n    \\\"success\\\": true,\\n    \\\"message\\\": \\\"User created successfully\\\",\\n    \\\"data\\\": {\\n        \\\"userId\\\": \\\"...\\\",\\n        \\\"name\\\": \\\"New User\\\",\\n        \\\"email\\\": \\\"newuser@example.com\\\",\\n        \\\"isAccountVerified\\\": true\\n    }\\n}\\n```"
            },
            "response": []
        }
    ],
    "description": "Admin operations endpoints"
};

data.item.push(adminFolder);
data.info.description = data.info.description.replace('### Profile', '### Admin\\n- **POST /api/admin/users** - Create new Admin or User account\\n\\n### Profile');

fs.writeFileSync(file, JSON.stringify(data, null, "\t"));
console.log("Postman collection updated successfully.");
