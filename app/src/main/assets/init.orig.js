var urlParams = new URLSearchParams(window.location.search);
var lat = urlParams.get('lat') ?? 12;
var lng = urlParams.get('lng') ?? 15;
var zoom = urlParams.get('zoom') ?? 12;
var provider = urlParams.get('provider') ?? 'OpenStreetMap';

var map = L.map('map').setView([lat, lng], zoom);
var popup = L.popup();
var alreadyRunning = false;

var searchInput = document.getElementById("address_input");
    searchInput.addEventListener("keypress", function(event) {
      if (event.key === "Enter") {
        event.preventDefault();
        searchForAddress();
      }
    });

L.tileLayer.provider(provider, {
    className: 'map-tiles',
}).addTo(map);

var icon = L.icon({
    iconUrl: 'marker-icon-2x.png',
    shadowUrl: 'marker-shadow.png',

    iconSize:     [27, 44], // size of the icon
    shadowSize:   [50, 64], // size of the shadow
    iconAnchor:   [14, 48], // point of the icon which will correspond to marker's location
    shadowAnchor: [17, 68 ],  // the same for the shadow
    popupAnchor:  [0, 0] // point from which the popup should open relative to the iconAnchor
});

async function searchForAddress() {
    var coords = await searchAddress(searchInput.value);
    if (coords === undefined || coords.length < 2) return;
    var lat = coords[0];
    var lng = coords[1];

    var mapEvent = {
        latlng: L.latLng(lat, lng)
    };
    setOnMap(lat, lng);
    onMapClick(mapEvent);
}

function onMapClick(e) {
    if (typeof mapMarker != 'undefined')
        map.removeLayer(mapMarker);
    mapMarker = L.marker(e.latlng, {icon: icon}).addTo(map);
    var wrap = e.latlng.wrap().toString();
    Android.setPosition(wrap);
}

function onZoomEnd(e) {
    Android.setZoom(map.getZoom());
}

function setOnMap(aLat, aLng) {
    if (typeof mapMarker != 'undefined')
        map.removeLayer(mapMarker);
    zoom = map.getZoom();
    map.setView(new L.LatLng(aLat, aLng), zoom);
    mapMarker = L.marker([aLat, aLng], {icon: icon}).addTo(map);
    alreadyRunning = true;
    //alert(alreadyRunning);
}

map.on('contextmenu', onMapClick);
map.on('zoomend', onZoomEnd);

mapMarker = L.marker([lat, lng], {icon: icon}).addTo(map);
