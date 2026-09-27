const API_URL = "http://127.0.0.1:5000";


// ==========================================
// REGISTER
// ==========================================

const registerForm = document.getElementById("registerForm");

if (registerForm) {

    registerForm.addEventListener("submit", async function (event) {

        event.preventDefault();

        const name = document.getElementById("registerName").value;
        const email = document.getElementById("registerEmail").value;
        const password = document.getElementById("registerPassword").value;

        try {

            const response = await fetch(`${API_URL}/api/register`, {

                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                credentials: "include",

                body: JSON.stringify({
                    name: name,
                    email: email,
                    password: password
                })

            });

            const data = await response.json();

            console.log("Register response:", data);

            if (data.success) {

                alert("Registration successful!");

                window.location.href = "login.html";

            } else {

                alert(data.message || "Registration failed");

            }

        } catch (error) {

            console.error("Registration error:", error);

            alert("Cannot connect to server.");

        }

    });

}



// ==========================================
// LOGIN
// ==========================================

const loginForm = document.getElementById("loginForm");

if (loginForm) {

    loginForm.addEventListener("submit", async function (event) {

        event.preventDefault();

        // IMPORTANT:
        // These IDs match login.html

        const email = document.getElementById("loginEmail").value;

        const password = document.getElementById("loginPassword").value;


        try {

            const response = await fetch(`${API_URL}/api/login`, {

                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                credentials: "include",

                body: JSON.stringify({
                    email: email,
                    password: password
                })

            });


            const data = await response.json();

            console.log("Login response:", data);


            if (data.success) {

                alert("Login successful!");

                window.location.href = "index.html";

            } else {

                alert(data.message || "Login failed");

            }


        } catch (error) {

            console.error("Login error:", error);

            alert("Cannot connect to server.");

        }

    });

}