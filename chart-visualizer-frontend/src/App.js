
import React, { useState, useEffect } from 'react';
import ChartConfigurator from './components/ChartConfigurator';
import Auth from './components/Auth'; 
import axios from 'axios'; 
import './App.css'; 

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false); // Kimlik doğrulama durumu
  const [jwtToken, setJwtToken] = useState(null); // JWT token'ı

  // Uygulama yüklendiğinde localStorage'da JWT kontrol et
  useEffect(() => {
    const storedJwt = localStorage.getItem('jwtToken');
    if (storedJwt) {
      setJwtToken(storedJwt);
      setIsAuthenticated(true);
    }
  }, []); 

  // JWT değiştiğinde veya kimlik doğrulama durumu değiştiğinde Axios'u yapılandır
  useEffect(() => {
    if (jwtToken) {
      // Axios'un her isteğine Authorization başlığını ekle
      axios.defaults.headers.common['Authorization'] = `Bearer ${jwtToken}`;
    } else {
      // JWT yoksa Authorization başlığını kaldır
      delete axios.defaults.headers.common['Authorization'];
    }
  }, [jwtToken]);

  // Login başarılı olduğunda çalışacak fonksiyon
  const handleLoginSuccess = (token) => {
    setJwtToken(token);
    setIsAuthenticated(true);
  };

  // Logout fonksiyonu
  const handleLogout = () => {
    localStorage.removeItem('jwtToken'); // localStorage'dan JWT'yi sil
    setJwtToken(null);
    setIsAuthenticated(false);
    // İsteğe bağlı: Sayfayı yenile veya ChartConfigurator'ın state'lerini sıfırla
    window.location.reload();
  };

  return (
    <div className="App">
      <header className="App-header">
        <h1>Dinamik Grafik Oluşturucu</h1>
        {isAuthenticated && (
          <button onClick={handleLogout} className="logout-button">Çıkış Yap</button>
        )}
      </header>
      <main>
        {!isAuthenticated ? (
          <Auth onLoginSuccess={handleLoginSuccess} /> // Giriş yapılmadıysa Auth bileşenini göster
        ) : (
          <ChartConfigurator /> // Giriş yapıldıysa ChartConfigurator'ı göster
        )}
      </main>
    </div>
  );
}

export default App;