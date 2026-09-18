// Configuration - Update this with your backend server's IP address
const API_URL = 'http://localhost:2222/api'; // Change this to your backend server's IP

// Elements
const todayVisitorsEl = document.getElementById('todayVisitors');
const currentVisitorsEl = document.getElementById('currentVisitors');
const totalFlatsEl = document.getElementById('totalFlats');
const visitorsGridEl = document.getElementById('visitorsGrid');
const searchInput = document.getElementById('searchInput');
const modal = document.getElementById('visitorModal');
const closeBtn = document.querySelector('.close-btn');

// Modal elements
const modalImage = document.getElementById('modalImage');
const modalName = document.getElementById('modalName');
const modalFlat = document.getElementById('modalFlat');
const modalTime = document.getElementById('modalTime');
const modalPhone = document.getElementById('modalPhone');
const modalPurpose = document.getElementById('modalPurpose');

// Fetch visitor data
async function fetchVisitorData() {
    try {
        const response = await fetch(`${API_URL}/visitors`);
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const data = await response.json();
        updateDashboard(data);
    } catch (error) {
        console.error('Error fetching visitor data:', error);
        showError();
    }
}

// Update dashboard with visitor data
function updateDashboard(data) {
    // Calculate statistics
    const today = new Date().toISOString().split('T')[0];
    const todayVisitors = data.filter(visitor => 
        visitor.timestamp && visitor.timestamp.includes(new Date().toLocaleDateString())
    ).length;

    const currentVisitors = data.length; // All visitors in current month

    const uniqueFlats = new Set();
    data.forEach(visitor => {
        if (visitor.flatNumbers) {
            const flats = typeof visitor.flatNumbers === 'string' ? visitor.flatNumbers.split(',') : [visitor.flatNumbers];
            flats.forEach(flat => uniqueFlats.add(flat.trim()));
        }
    });

    // Update statistics
    todayVisitorsEl.textContent = todayVisitors;
    currentVisitorsEl.textContent = currentVisitors;
    totalFlatsEl.textContent = uniqueFlats.size;

    // Update visitors grid with most recent first
    updateVisitorsGrid(data.reverse().slice(0, 50)); // Show last 50 visitors
}

// Create visitor cards
function updateVisitorsGrid(visitors) {
    visitorsGridEl.innerHTML = '';
    if (visitors.length > 0) {
        visitors.forEach(visitor => {
            const card = createVisitorCard(visitor);
            visitorsGridEl.appendChild(card);
        });
    } else {
        showNoVisitors();
    }
}

// Create a single visitor card
function createVisitorCard(visitor) {
    const card = document.createElement('div');
    card.className = 'visitor-card';
    
    // Construct image URL using the imagePath from backend
    const imageUrl = visitor.imagePath ? `http://localhost:2222/data/${new Date().getFullYear()}/${new Date().toLocaleString('default', { month: 'long' })}/Images/${visitor.imagePath}` : 'https://via.placeholder.com/150?text=No+Image';
    
    card.innerHTML = `
        <img src="${imageUrl}" alt="${visitor.name}" class="visitor-image" onerror="this.src='https://via.placeholder.com/150?text=No+Image'">
        <div class="visitor-info">
            <h3>${visitor.name}</h3>
            <p><i class="fas fa-home"></i> Flat: ${visitor.flatNumbers}</p>
            <p><i class="fas fa-clock"></i> ${formatTime(visitor.timestamp)}</p>
        </div>
    `;

    // Add click event to show modal
    card.addEventListener('click', () => showVisitorDetails(visitor));
    return card;
}

// Show visitor details in modal
function showVisitorDetails(visitor) {
    modalImage.src = visitor.imagePath ? `http://localhost:2222/data/${new Date().getFullYear()}/${new Date().toLocaleString('default', { month: 'long' })}/Images/${visitor.imagePath}` : 'https://via.placeholder.com/150?text=No+Image';
    modalImage.onerror = () => {
        modalImage.src = 'https://via.placeholder.com/150?text=No+Image';
    };
    modalName.textContent = visitor.name;
    modalFlat.textContent = `Visiting Flat: ${visitor.flatNumbers}`;
    modalTime.textContent = `Arrival: ${formatTime(visitor.timestamp)}`;
    modalPhone.textContent = `Phone: ${visitor.phoneNumber || 'Not provided'}`;
    modalPurpose.textContent = 'Purpose: Visitor Alert';
}

// Helper function to format timestamps
function formatTime(timestamp) {
    if (!timestamp) return 'Unknown';
    const date = new Date(timestamp);
    return date.toLocaleString();
}

// Show error message
function showError() {
    visitorsGridEl.innerHTML = `
        <div class="error-message">
            Failed to load visitor data. Please check your connection to the backend server.
        </div>
    `;
    todayVisitorsEl.textContent = 'Error';
    currentVisitorsEl.textContent = 'Error';
    totalFlatsEl.textContent = 'Error';
}

// Show message when no visitors
function showNoVisitors() {
    visitorsGridEl.innerHTML = `
        <div class="no-data-message">
            No visitors recorded yet.
        </div>
    `;
}

// Search functionality
searchInput.addEventListener('input', (e) => {
    const searchTerm = e.target.value.toLowerCase();
    const visitorCards = visitorsGridEl.getElementsByClassName('visitor-card');
    
    Array.from(visitorCards).forEach(card => {
        const visitorName = card.querySelector('h3').textContent.toLowerCase();
        const visitorFlat = card.querySelector('.visitor-info p').textContent.toLowerCase();
        
        if (visitorName.includes(searchTerm) || visitorFlat.includes(searchTerm)) {
            card.style.display = 'block';
        } else {
            card.style.display = 'none';
        }
    });
});

// Modal close button
closeBtn.addEventListener('click', () => {
    modal.style.display = 'none';
});

// Close modal when clicking outside
window.addEventListener('click', (e) => {
    if (e.target === modal) {
        modal.style.display = 'none';
    }
});

// Initial load
fetchVisitorData();

// Refresh data every 30 seconds
setInterval(fetchVisitorData, 30000); 