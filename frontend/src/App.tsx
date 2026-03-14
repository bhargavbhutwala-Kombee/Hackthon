import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import Login from './pages/Login';
import Register from './pages/Register';
import DashboardLayout from './pages/DashboardLayout';
import Products from './pages/Products';

const PrivateRoute = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? children : <Navigate to="/login" replace />;
};

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        
        <Route path="/dashboard" element={
          <PrivateRoute>
            <DashboardLayout />
          </PrivateRoute>
        }>
          <Route index element={<Navigate to="products" replace />} />
          <Route path="products" element={<Products />} />
          {/* <Route path="orders" element={<Orders />} /> */}
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
