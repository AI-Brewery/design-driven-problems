const API_URL = "http://127.0.0.1:5000";

const tableBody =
    document.getElementById("myUrlsTableBody");

const selectAll =
    document.getElementById("selectAll");

const deleteBtn =
    document.getElementById("deleteBtn");


/* ==========================================
   LOAD MY URLS
========================================== */

async function loadMyUrls() {

    try {

        const response = await fetch(
            `${API_URL}/api/my-urls`,
            {
                method: "GET",
                credentials: "include"
            }
        );


        const data = await response.json();


        /*
         * User is not logged in
         */

        if (response.status === 401) {

            alert("Please login first.");

            window.location.href = "login.html";

            return;
        }


        if (!response.ok) {

            alert(data.message);

            return;
        }


        tableBody.innerHTML = "";


        /*
         * No URLs
         */

        if (data.urls.length === 0) {

            tableBody.innerHTML = `
                <tr>
                    <td colspan="6" style="text-align:center;">
                        You haven't created any URLs yet.
                    </td>
                </tr>
            `;

            return;
        }


        /*
         * Add each URL
         */

        data.urls.forEach(url => {

            const row =
                document.createElement("tr");


            row.innerHTML = `

                <td class="checkbox-column">

                    <input
                        type="checkbox"
                        class="url-checkbox"
                        value="${url.id}"
                    >

                </td>


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
                    ${url.type}
                </td>


                <td>
                    ${formatDate(url.created_at)}
                </td>


                <td>
                    ${url.expiry_date}
                </td>

            `;


            tableBody.appendChild(row);

        });


    } catch (error) {

        console.error(error);

        alert(
            "Could not connect to the backend."
        );

    }
}


/* ==========================================
   SELECT ALL
========================================== */

selectAll.addEventListener(
    "change",
    () => {

        const checkboxes =
            document.querySelectorAll(
                ".url-checkbox"
            );


        checkboxes.forEach(
            checkbox => {
                checkbox.checked =
                    selectAll.checked;
            }
        );

    }
);


/* ==========================================
   DELETE SELECTED URLS
========================================== */

deleteBtn.addEventListener(
    "click",
    async () => {

        const selected =
            document.querySelectorAll(
                ".url-checkbox:checked"
            );


        if (selected.length === 0) {

            alert(
                "Please select at least one URL."
            );

            return;
        }


        const confirmed =
            confirm(
                `Delete ${selected.length} selected URL(s)?`
            );


        if (!confirmed) {
            return;
        }


        /*
         * Delete one by one
         */

        for (const checkbox of selected) {

            const urlId = checkbox.value;


            try {

                const response =
                    await fetch(
                        `${API_URL}/api/urls/${urlId}`,
                        {
                            method: "DELETE",

                            credentials: "include"
                        }
                    );


                if (!response.ok) {

                    const data =
                        await response.json();

                    alert(data.message);

                    return;
                }


            } catch (error) {

                console.error(error);

                alert(
                    "Could not delete the URL."
                );

                return;
            }

        }


        alert(
            "Selected URLs deleted successfully!"
        );


        /*
         * Reload table
         */

        selectAll.checked = false;

        loadMyUrls();

    }
);


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

loadMyUrls();