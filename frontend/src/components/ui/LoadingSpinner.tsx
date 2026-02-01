import { motion } from "framer-motion";
import AcadexisLogo from '@/assets/AcadexisLogo.png';

interface LoadingSpinnerProps {
    size?: "sm" | "md" | "lg";
    showMessage?: boolean;
    message?: string;
    subMessage?: string;
}

export const LoadingSpinner = ({
    size = "lg",
    showMessage = true,
    message = "Loading...",
    subMessage = "This may take 20-30 seconds..."
}: LoadingSpinnerProps) => {
    const sizeClasses = {
        sm: { container: "w-20 h-20", icon: "w-8 h-8", ring: 80 },
        md: { container: "w-28 h-28", icon: "w-10 h-10", ring: 112 },
        lg: { container: "w-36 h-36", icon: "w-22 h-22", ring: 144 },
    };

    const config = sizeClasses[size];

    return (
        <div className="flex flex-col items-center gap-6">
            {/* Main spinner */}
            <div className={`relative ${config.container}`}>
                {/* Outer ring */}
                <div
                    className="absolute inset-0 rounded-full border-4 border-primary-500/20"
                />

                {/* Animated gradient ring */}
                <motion.div
                    animate={{ rotate: 360 }}
                    transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
                    className="absolute inset-0"
                >
                    <svg className="w-full h-full" viewBox={`0 0 ${config.ring} ${config.ring}`}>
                        <defs>
                            <linearGradient id="spinnerGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                                <stop offset="0%" stopColor="#3b82f6" />
                                <stop offset="50%" stopColor="#38bdf8" />
                                <stop offset="100%" stopColor="#a855f7" />
                            </linearGradient>
                        </defs>
                        <circle
                            cx={config.ring / 2}
                            cy={config.ring / 2}
                            r={(config.ring / 2) - 8}
                            fill="none"
                            stroke="url(#spinnerGradient)"
                            strokeWidth="4"
                            strokeLinecap="round"
                            strokeDasharray={`${config.ring * 2} ${config.ring}`}
                        />
                    </svg>
                </motion.div>

                {/* Pulsing background */}
                <motion.div
                    animate={{ scale: [1, 1.1, 1], opacity: [0.3, 0.5, 0.3] }}
                    transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
                    className="absolute inset-4 rounded-full bg-primary-500/10"
                />

                {/* Center icon */}
                <div className="absolute inset-0 flex items-center justify-center">
                    <motion.div
                        animate={{ scale: [1, 1.1, 1] }}
                        transition={{ duration: 1.5, repeat: Infinity, ease: "easeInOut" }}
                        className="w-16 h-16 rounded-2xl bg-white flex items-center justify-center shadow-glow overflow-hidden"
                    >
                        <img src={AcadexisLogo} alt="Logo" className="w-10 h-10 object-contain" />
                    </motion.div>
                </div>

                {/* Floating particles */}
                {[...Array(4)].map((_, i) => (
                    <motion.div
                        key={i}
                        className="absolute w-2 h-2 rounded-full bg-primary-500"
                        animate={{
                            x: [0, Math.cos((i * 90 * Math.PI) / 180) * 60],
                            y: [0, Math.sin((i * 90 * Math.PI) / 180) * 60],
                            opacity: [0, 1, 0],
                            scale: [0.5, 1, 0.5],
                        }}
                        transition={{
                            duration: 2.5,
                            repeat: Infinity,
                            delay: i * 0.5,
                            ease: "easeInOut",
                        }}
                        style={{
                            left: "50%",
                            top: "50%",
                            transform: "translate(-50%, -50%)",
                        }}
                    />
                ))}
            </div>

            {/* Message */}
            {showMessage && (
                <div className="text-center">
                    <motion.p
                        animate={{ opacity: [0.7, 1, 0.7] }}
                        transition={{ duration: 2, repeat: Infinity }}
                        className="text-xl font-semibold text-gray-900"
                    >
                        {message}
                    </motion.p>
                    {subMessage && (
                        <p className="text-sm text-muted-foreground mt-2">
                            {subMessage}
                        </p>
                    )}
                </div>
            )}
        </div>
    );
};
