import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api';
import { User, Mail, Lock } from 'lucide-react';

export default function Register() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await api.post('/auth/register', { username, email, password });
      navigate('/login');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden">
      <div className="absolute top-1/4 right-[10%] w-96 h-96 bg-accent/20 rounded-full blur-3xl animate-pulse" />
      <div className="absolute bottom-[10%] left-[5%] w-96 h-96 bg-primary/20 rounded-full blur-3xl" />
      
      <div className="glass-panel w-full max-w-md p-8 relative z-10 animate-fade-in-up">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold bg-gradient-to-r from-secondary to-primary bg-clip-text text-transparent">
            Create Account
          </h1>
          <p className="text-textMuted mt-2">Join the Orderly platform</p>
        </div>

        {error && (
          <div className="mb-6 p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleRegister} className="space-y-5">
          <div className="space-y-1 relative">
            <User className="absolute left-4 top-[38px] -translate-y-1/2 w-5 h-5 text-textMuted" />
            <label className="text-sm font-medium text-textMuted ml-1">Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="glass-input w-full pl-12"
              placeholder="e.g. john_doe"
              required
            />
          </div>

          <div className="space-y-1 relative">
            <Mail className="absolute left-4 top-[38px] -translate-y-1/2 w-5 h-5 text-textMuted" />
            <label className="text-sm font-medium text-textMuted ml-1">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="glass-input w-full pl-12"
              placeholder="name@company.com"
              required
            />
          </div>

          <div className="space-y-1 relative">
            <Lock className="absolute left-4 top-[38px] -translate-y-1/2 w-5 h-5 text-textMuted" />
            <label className="text-sm font-medium text-textMuted ml-1">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="glass-input w-full pl-12"
              placeholder="••••••••"
              required
            />
          </div>

          <button type="submit" disabled={loading} className="glass-button w-full flex justify-center mt-6">
            {loading ? 'Creating...' : 'Register'}
          </button>
        </form>

        <p className="mt-8 text-center text-sm text-textMuted">
          Already have an account?{' '}
          <Link to="/login" className="text-secondary hover:text-primary font-medium transition-colors">
            Sign In
          </Link>
        </p>
      </div>
    </div>
  );
}
