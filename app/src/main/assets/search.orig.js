async function searchAddress(query) {
    // Define the Nominatim API endpoint
    const api_url = "https://nominatim.openstreetmap.org/search";

    var success = false;
    var result = ["-1", "-1"];

    // Parameters for the search
    const params = {
        q: query,
        format: "json",
        limit: "1"
    };

    // Construct the URL with query parameters
    const url = new URL(api_url);
    url.search = new URLSearchParams(params).toString();

    await fetch(url)
        .then(response => response.json())
        .then(data => {
            result = getCoords(data);
            success = true;
        })
        .catch(error => {
            console.error("Error:", error);
        });

        return success ? result : undefined;
}

function getCoords(data)
{
    if(data.length > 0)
    {
        var firstResult = data[0];
        var lat = firstResult.lat;
        var long = firstResult.lon;

        return [lat, long];
    }
}
