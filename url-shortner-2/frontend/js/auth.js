const API_URL = "http://127.0.0.1:5000";


/* ==========================================
   REGISTRATION
========================================== */

const registerForm = document.getElementById("registerForm");

if (registerForm) {

    registerForm.addEventListener("submit", async (event) => {

        event.preventDefault();


        const name =
            document.getElementById("name").value.trim();

        const email =
            document.getElementById("email").value.trim();

        const password =
            document.getElementById("password").value;


        if (!name || !email || !password) {

            alert("Please fill in all fields.");

            return;
        }


        try {

            const response = await fetch(
                `${API_URL}/api/register`,
                {
                    method: "POST",

                    headers: {
                        "Content-Type": "application/json"
                    },

                    body: JSON.stringify({
                        name: name,
                        email: email,
                        password: password
                    })
                }
            );


            const data = await response.json();


            if (!response.ok) {

                alert(data.message);

                return;
            }


            alert("Registration successful!");

            window.location.href = "login.html";


        } catch (error) {

            console.error(error);

            alert("Could not connect to the server.");

        }

    });

}


/* ==========================================
   LOGIN
========================================== */

const loginForm = document.getElementById("loginForm");

if (loginForm) {

    loginForm.addEventListener("submit", async (event) => {

        event.preventDefault();


        const email =
            document.getElementById("loginEmail").value.trim();

        const password =
            document.getElementById("loginPassword").value;


        if (!email || !password) {

            alert("Please enter email and password.");

            return;
        }


        try {

            const response = await fetch(
                `${API_URL}/api/login`,
                {
                    method: "POST",

                    headers: {
                        "Content-Type": "application/json"
                    },

                    credentials: "include",

                    body: JSON.stringify({
                        email: email,
                        password: password
                    })
                }
            );


            const data = await response.json();


            if (!response.ok) {

                alert(data.message);

                return;
            }


            console.log("Logged in user:", data.user);

            alert(`Welcome, ${data.user.name}!`);


            /*
             * Go back to Home after login
             */

            window.location.href = "index.html";


        } catch (error) {

            console.error(error);

            alert("Could not connect to the server.");

        }

    });

}