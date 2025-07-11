// src/components/Auth.js
import React, { useState } from 'react';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/auth'; // Backend'deki Auth Controller adresi

function Auth({ onLoginSuccess }) { // onLoginSuccess prop'u ile parent component'a bildirim göndereceğiz
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('adminpass');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault(); 

    setLoading(true);
    setError('');

    try {
      const response = await axios.post(`${API_BASE_URL}/login`, { username, password });
      const { jwt } = response.data; // Backend'den gelen JWT'yi al

      localStorage.setItem('jwtToken', jwt); // JWT'yi localStorage'a kaydet

      // Giriş başarılı olduğunda parent component'ı bilgilendir
      onLoginSuccess(jwt);

    } catch (err) {
      console.error('Login Error:', err);
      setError('Giriş başarısız: ' + (err.response?.data || err.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <h2>Giriş Yap</h2>
      <form onSubmit={handleLogin} className="login-form">
        <div className="form-group">
          <label htmlFor="username">Kullanıcı Adı:</label>
          <input
            type="text"
            id="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>
        <div className="form-group">
          <label htmlFor="password">Parola:</label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <button type="submit" disabled={loading}>
          {loading ? 'Giriş Yapılıyor...' : 'Giriş Yap'}
        </button>
      </form>
      {error && <p className="error-message">{error}</p>}
    </div>
  );
}

export default Auth;