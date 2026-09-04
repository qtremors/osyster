// Tailwind Configuration for Osyster Slate Tech Design System
tailwind.config = {
    theme: {
        extend: {
            fontFamily: {
                sans: ['Inter', 'Roboto', 'sans-serif'],
                display: ['Space Grotesk', 'Outfit', 'sans-serif'],
                mono: ['JetBrains Mono', 'monospace'],
            },
            colors: {
                slate: {
                    950: '#080C0E',
                    900: '#0C1115', // BackgroundDark
                    850: '#141C22', // SurfaceDark
                    800: '#1C2730', // SurfaceContainerHighDark
                    700: '#283642',
                    600: '#3D4E5E',
                },
                brand: {
                    cyan: '#00E6FF',
                    cyanDark: '#00363D',
                    cyanContainer: '#004E58',
                    cyanLight: '#B2F5FF',
                    amber: '#FFB300',
                    amberDark: '#452B00',
                    amberContainer: '#623F00',
                    coral: '#FF5252',
                    coralDark: '#5F0006',
                    text: '#E1E2E5',
                    muted: '#8B9198',
                }
            },
            animation: {
                'float': 'float 6s ease-in-out infinite',
                'pulse-slow': 'pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite',
            },
            keyframes: {
                float: {
                    '0%, 100%': { transform: 'translateY(0)' },
                    '50%': { transform: 'translateY(-10px)' },
                }
            },
            transitionTimingFunction: {
                'spring': 'cubic-bezier(0.34, 1.56, 0.64, 1)',
            }
        }
    }
};

// Navbar surface elevation on scroll
const navbar = document.getElementById('navbar');
window.addEventListener('scroll', () => {
    if (window.scrollY > 20) {
        navbar.classList.remove('bg-slate-900');
        navbar.classList.add('bg-slate-850/90', 'backdrop-blur-md', 'shadow-2xl', 'border-b', 'border-slate-800');
    } else {
        navbar.classList.add('bg-slate-900');
        navbar.classList.remove('bg-slate-850/90', 'backdrop-blur-md', 'shadow-2xl', 'border-b', 'border-slate-800');
    }
});

// FAQ Accordion Toggle
function toggleFaq(element) {
    const content = element.querySelector('.faq-content');
    const icon = element.querySelector('.faq-icon');

    document.querySelectorAll('.faq-content').forEach(el => {
        if (el !== content) el.classList.remove('open');
    });
    document.querySelectorAll('.faq-icon').forEach(el => {
        if (el !== icon) el.classList.remove('rotate');
    });

    content.classList.toggle('open');
    icon.classList.toggle('rotate');
}

// Scroll Reveal Observer
document.addEventListener('DOMContentLoaded', () => {
    lucide.createIcons();

    const observer = new IntersectionObserver((entries, obs) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('opacity-100', 'translate-y-0');
                entry.target.classList.remove('opacity-0', 'translate-y-8');
                obs.unobserve(entry.target);
            }
        });
    }, { threshold: 0.1 });

    document.querySelectorAll('.reveal').forEach(el => observer.observe(el));

    initSparklineCanvas();
});

// Counter Animation
const numberFormatter = new Intl.NumberFormat();
const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

function animateCounter(element, target) {
    if (!element || !Number.isFinite(target)) return;

    const endValue = Math.max(0, Math.trunc(target));
    if (reduceMotion.matches || endValue === 0) {
        element.innerText = numberFormatter.format(endValue);
        return;
    }

    const duration = 800;
    const startTime = performance.now();

    function update(currentTime) {
        const progress = Math.min((currentTime - startTime) / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        element.innerText = numberFormatter.format(Math.round(endValue * eased));

        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }

    requestAnimationFrame(update);
}

function sumReleaseDownloads(releases) {
    return releases.reduce((total, rel) => {
        const assetSum = Array.isArray(rel.assets)
            ? rel.assets.reduce((sum, a) => sum + (a.download_count || 0), 0)
            : 0;
        return total + assetSum;
    }, 0);
}

async function fetchTotalReleaseDownloads() {
    let nextUrl = 'https://api.github.com/repos/qtremors/osyster/releases?per_page=100';
    let totalDownloads = 0;

    while (nextUrl) {
        const response = await fetch(nextUrl);
        if (!response.ok) throw new Error(`GitHub releases error: ${response.status}`);

        const releases = await response.json();
        totalDownloads += sumReleaseDownloads(releases);

        const linkHeader = response.headers.get('link');
        const nextMatch = linkHeader?.split(',').find(l => l.includes('rel="next"'));
        nextUrl = nextMatch?.match(/<([^>]+)>/)?.[1] || '';
    }

    return totalDownloads;
}

// Fetch GitHub Stats
async function fetchGitHubStats() {
    const [repoResult, releaseResult, totalDownloadsResult] = await Promise.allSettled([
        fetch('https://api.github.com/repos/qtremors/osyster'),
        fetch('https://api.github.com/repos/qtremors/osyster/releases/latest'),
        fetchTotalReleaseDownloads()
    ]);

    try {
        const repoRes = repoResult.status === 'fulfilled' ? repoResult.value : null;
        if (repoRes?.ok) {
            const data = await repoRes.json();
            if (data.stargazers_count !== undefined) {
                animateCounter(document.getElementById('gh-stars'), data.stargazers_count);
                animateCounter(document.getElementById('gh-forks'), data.forks_count);
            }
        }
    } catch (e) {
        console.error('Error fetching repo stats:', e);
    }

    try {
        const releaseRes = releaseResult.status === 'fulfilled' ? releaseResult.value : null;
        if (releaseRes?.ok) {
            const release = await releaseRes.json();
            if (release.tag_name) {
                document.querySelectorAll('.download-btn-text').forEach(el => {
                    el.innerText = `Download ${release.tag_name}`;
                });
            }
            const latestDownloads = sumReleaseDownloads([release]);
            animateCounter(document.getElementById('gh-latest-downloads'), latestDownloads);
        }
    } catch (e) {
        console.error('Error fetching latest release:', e);
    }

    if (totalDownloadsResult.status === 'fulfilled') {
        animateCounter(document.getElementById('gh-total-downloads'), totalDownloadsResult.value);
    }
}

fetchGitHubStats();

// Mobile Menu Toggle
const mobileMenuBtn = document.getElementById('mobile-menu-btn');
const mobileMenu = document.getElementById('mobile-menu');
let isMobileMenuOpen = false;

if (mobileMenuBtn && mobileMenu) {
    const mobileMenuIcon = document.getElementById('mobile-menu-icon');

    function toggleMobileMenu() {
        isMobileMenuOpen = !isMobileMenuOpen;
        if (isMobileMenuOpen) {
            mobileMenu.classList.remove('translate-x-full');
            mobileMenu.classList.add('translate-x-0');
            if (mobileMenuIcon) mobileMenuIcon.setAttribute('data-lucide', 'x');
            document.body.style.overflow = 'hidden';
        } else {
            mobileMenu.classList.add('translate-x-full');
            mobileMenu.classList.remove('translate-x-0');
            if (mobileMenuIcon) mobileMenuIcon.setAttribute('data-lucide', 'menu');
            document.body.style.overflow = '';
        }
        lucide.createIcons();
    }

    mobileMenuBtn.addEventListener('click', toggleMobileMenu);

    document.querySelectorAll('.mobile-link').forEach(link => {
        link.addEventListener('click', () => {
            if (isMobileMenuOpen) toggleMobileMenu();
        });
    });
}

// Live Simulated Sparkline Canvas
function initSparklineCanvas() {
    const canvas = document.getElementById('sparkline-canvas');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();

    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);

    const points = [18, 22, 19, 35, 42, 38, 55, 48, 62, 59, 45, 52, 68, 74, 58, 63, 71, 65, 82, 77];

    function render() {
        const width = rect.width;
        const height = rect.height;

        ctx.clearRect(0, 0, width, height);

        // Draw sparkline curve
        ctx.beginPath();
        const step = width / (points.length - 1);

        points.forEach((val, i) => {
            const x = i * step;
            const y = height - (val / 100) * (height - 16) - 8;
            if (i === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        });

        ctx.strokeStyle = '#00E6FF';
        ctx.lineWidth = 2.5;
        ctx.lineJoin = 'round';
        ctx.lineCap = 'round';
        ctx.stroke();

        // Area fill
        ctx.lineTo(width, height);
        ctx.lineTo(0, height);
        ctx.closePath();

        const grad = ctx.createLinearGradient(0, 0, 0, height);
        grad.addColorStop(0, 'rgba(0, 230, 255, 0.28)');
        grad.addColorStop(1, 'rgba(0, 230, 255, 0.0)');
        ctx.fillStyle = grad;
        ctx.fill();

        // Pulsing head point
        const lastX = width;
        const lastVal = points[points.length - 1];
        const lastY = height - (lastVal / 100) * (height - 16) - 8;

        ctx.beginPath();
        ctx.arc(lastX, lastY, 4, 0, Math.PI * 2);
        ctx.fillStyle = '#00E6FF';
        ctx.fill();

        const cpuReadout = document.getElementById('live-cpu-val');
        if (cpuReadout) {
            cpuReadout.innerText = `${Math.round(lastVal)}%`;
        }
    }

    render();

    // Subtle realtime shift every 1.5s
    setInterval(() => {
        const lastVal = points[points.length - 1];
        const delta = (Math.random() - 0.48) * 14;
        const nextVal = Math.min(94, Math.max(12, lastVal + delta));
        points.shift();
        points.push(nextVal);
        render();
    }, 1500);
}
