const API_URL = "http://127.0.0.1:5000";

const shortenBtn = document.getElementById("shortenBtn");
const urlInput = document.getElementById("urlInput");
const urlTableBody = document.getElementById("urlTableBody");


/* ==========================================
   SHORTEN URL
========================================== */

shortenBtn.addEventListener("click", async () => {

    const originalUrl = urlInput.value.trim();

    if (!originalUrl) {
        alert("Please enter a URL.");
        return;
    }


    // Get Public / Private selection
    const selectedType =
        document.querySelector(
            'input[name="urlType"]:checked'
        ).value;


    // Get expiry selection
    const expiryValue =
        document.getElementById("expiryDate").value;


    // Default: no expiry
    let expiryDate = null;


    // Calculate expiry date
    if (expiryValue) {

        const date = new Date();

        date.setDate(
            date.getDate() + Number(expiryValue)
        );

        expiryDate = date.toISOString();
    }


    try {

        const response = await fetch(
            `${API_URL}/api/shorten`,
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                credentials: "include",

                body: JSON.stringify({

                    original_url:
                        originalUrl,

                    url_type:
                        selectedType,

                    expiry_date:
                        expiryDate
                })
            }
        );


        const data = await response.json();


        if (!response.ok) {

            alert(data.message);

            return;
        }


        alert(
            "URL shortened successfully!"
        );


        // Clear input
        urlInput.value = "";


        // Reload URL table
        loadUrls();

    } catch (error) {

        console.error(error);

        alert(
            "Could not connect to the backend."
        );
    }

});


/* ==========================================
   LOAD PUBLIC URLS
========================================== */

async function loadUrls() {

    try {

        const response = await fetch(
            `${API_URL}/api/urls`,
            {
                credentials: "include"
            }
        );


        if (!response.ok) {
            return;
        }


        const data =
            await response.json();


        urlTableBody.innerHTML = "";


        data.urls.forEach(url => {

            const row =
                document.createElement("tr");


            row.innerHTML = `

                <td>

                    <a
                        href="${url.short_url}"
                        target="_blank"
                    >
                        ${url.short_url}
                    </a>

                </td>


                <td>
                    ${url.original_url}
                </td>


                <td>
                    ${formatDate(url.created_at)}
                </td>


                <td>
                    ${url.expiry_date}
                </td>

            `;


            urlTableBody.appendChild(row);

        });


    } catch (error) {

        console.error(error);

    }

}


/* ==========================================
   FORMAT DATE
========================================== */

function formatDate(dateString) {

    if (!dateString) {
        return "-";
    }


    const date =
        new Date(dateString);


    return date.toLocaleDateString();

}


/* ==========================================
   INITIAL LOAD
========================================== */

loadUrls();