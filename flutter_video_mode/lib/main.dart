import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'dart:ui';

void main() {
  runApp(const CinematicVideoApp());
}

class CinematicVideoApp extends StatelessWidget {
  const CinematicVideoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Streamify - Video Mode',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF000000),
        primaryColor: const Color(0xFFE50914), // Cinematic Red
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFFE50914),
          secondary: Color(0xFF564D4D),
          surface: Color(0xFF121212),
        ),
        textTheme: GoogleFonts.montserratTextTheme(ThemeData.dark().textTheme),
        useMaterial3: true,
      ),
      home: const VideoModeScreen(),
    );
  }
}

class VideoModeScreen extends StatefulWidget {
  const VideoModeScreen({super.key});

  @override
  State<VideoModeScreen> createState() => _VideoModeScreenState();
}

class _VideoModeScreenState extends State<VideoModeScreen> {
  int _selectedIndex = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBodyBehindAppBar: true,
      extendBody: true,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        flexibleSpace: ClipRect(
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
            child: Container(color: Colors.black.withOpacity(0.2)),
          ),
        ),
        title: ShaderMask(
          shaderCallback: (bounds) => const LinearGradient(
            colors: [Color(0xFFE50914), Color(0xFFFF5252)],
          ).createShader(bounds),
          child: Text(
            'STREAMIFY',
            style: GoogleFonts.bebasNeue(
              fontSize: 32,
              letterSpacing: 2,
              color: Colors.white,
            ),
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.cast, color: Colors.white),
            onPressed: () {},
          ),
          IconButton(
            icon: const Icon(Icons.search, color: Colors.white, size: 28),
            onPressed: () {},
          ),
          const SizedBox(width: 8),
          Container(
            margin: const EdgeInsets.only(right: 16),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(4),
              image: const DecorationImage(
                image: NetworkImage('https://upload.wikimedia.org/wikipedia/commons/0/0b/Netflix-avatar.png'),
                fit: BoxFit.cover,
              ),
            ),
            width: 32,
            height: 32,
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            const HeroBackdrop(),
            const SizedBox(height: 20),
            _buildSectionHeader('Continue Watching for John'),
            const ContinueWatchingList(),
            const SizedBox(height: 20),
            _buildSectionHeader('Trending Now'),
            const MovieCategoryList(
              posterUrls: [
                'https://image.tmdb.org/t/p/w500/8Gxv9mYjtUB1zYXYwqvUrQvxtWg.jpg',
                'https://image.tmdb.org/t/p/w500/qNBAXBIQp35qy7AnSvCU1Iu9o98.jpg',
                'https://image.tmdb.org/t/p/w500/z0S78vEk6g0STTr6Q179Z9M4STP.jpg',
                'https://image.tmdb.org/t/p/w500/uS9mY7o97Sdnv79I9I4M2Iq4ccp.jpg',
              ],
            ),
            const SizedBox(height: 20),
            _buildSectionHeader('My List'),
            const MovieCategoryList(
              posterUrls: [
                'https://image.tmdb.org/t/p/w500/iuFNMSvS5v9f9iOBJ3dfmU0Pgn5.jpg',
                'https://image.tmdb.org/t/p/w500/fiVW0BtJmS7zU0fbm8zH6o946Eq.jpg',
                'https://image.tmdb.org/t/p/w500/1XS1oqL6BGvpo075i6X9S6t6D6.jpg',
              ],
            ),
            const SizedBox(height: 100), // Spacing for bottom nav
          ],
        ),
      ),
      bottomNavigationBar: _buildBottomNav(),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            title,
            style: const TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              letterSpacing: 0.5,
            ),
          ),
          const Icon(Icons.arrow_forward_ios, size: 14, color: Colors.grey),
        ],
      ),
    );
  }

  Widget _buildBottomNav() {
    return ClipRRect(
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
        child: Container(
          height: 85,
          decoration: BoxDecoration(
            color: Colors.black.withOpacity(0.8),
            border: Border(top: BorderSide(color: Colors.white.withOpacity(0.1), width: 0.5)),
          ),
          child: BottomNavigationBar(
            currentIndex: _selectedIndex,
            onTap: (index) => setState(() => _selectedIndex = index),
            backgroundColor: Colors.transparent,
            elevation: 0,
            type: BottomNavigationBarType.fixed,
            selectedItemColor: Colors.white,
            unselectedItemColor: Colors.grey,
            selectedFontSize: 12,
            unselectedFontSize: 12,
            items: const [
              BottomNavigationBarItem(icon: Icon(Icons.home_filled), label: 'Home'),
              BottomNavigationBarItem(icon: Icon(Icons.video_library_outlined), label: 'New & Hot'),
              BottomNavigationBarItem(icon: Icon(Icons.sentiment_very_satisfied), label: 'Fast Laughs'),
              BottomNavigationBarItem(icon: Icon(Icons.download_for_offline_outlined), label: 'Downloads'),
              BottomNavigationBarItem(icon: Icon(Icons.menu), label: 'More'),
            ],
          ),
        ),
      ),
    );
  }
}

class HeroBackdrop extends StatelessWidget {
  const HeroBackdrop({super.key});

  @override
  Widget build(BuildContext context) {
    return Stack(
      alignment: Alignment.bottomCenter,
      children: [
        // Background Image with Gradient
        Container(
          height: MediaQuery.of(context).size.height * 0.7,
          width: double.infinity,
          foregroundDecoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: [
                Colors.black.withOpacity(0.3),
                Colors.transparent,
                Colors.black.withOpacity(0.5),
                Colors.black,
              ],
              stops: const [0, 0.4, 0.7, 1],
            ),
          ),
          child: Image.network(
            'https://image.tmdb.org/t/p/original/mXLOHHc1ZcmwCYvpDCz2SbuG8Xp.jpg', // Dune: Part Two
            fit: BoxFit.cover,
          ),
        ),
        // Content
        Positioned(
          bottom: 20,
          child: Column(
            children: [
              // Logo/Title
              Text(
                'DUNE',
                style: GoogleFonts.bebasNeue(
                  fontSize: 70,
                  color: Colors.white,
                  letterSpacing: 10,
                ),
              ),
              Transform.translate(
                offset: const Offset(0, -15),
                child: Text(
                  'PART TWO',
                  style: GoogleFonts.montserrat(
                    fontSize: 14,
                    color: Colors.white70,
                    letterSpacing: 8,
                    fontWeight: FontWeight.w300,
                  ),
                ),
              ),
              const SizedBox(height: 10),
              // Tags
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _buildTag('Sci-Fi'),
                  _buildDot(),
                  _buildTag('Action'),
                  _buildDot(),
                  _buildTag('Epic'),
                  _buildDot(),
                  _buildTag('IMAX'),
                ],
              ),
              const SizedBox(height: 25),
              // Buttons
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _buildHeroButton(
                    icon: Icons.play_arrow,
                    label: 'Play',
                    onPressed: () {},
                    isPrimary: true,
                  ),
                  const SizedBox(width: 12),
                  _buildHeroButton(
                    icon: Icons.add,
                    label: 'My List',
                    onPressed: () {},
                    isPrimary: false,
                  ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildHeroButton({
    required IconData icon,
    required String label,
    required VoidCallback onPressed,
    required bool isPrimary,
  }) {
    return Container(
      width: 150,
      height: 45,
      decoration: BoxDecoration(
        color: isPrimary ? Colors.white : Colors.white.withOpacity(0.2),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onPressed,
          borderRadius: BorderRadius.circular(4),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, color: isPrimary ? Colors.black : Colors.white, size: 28),
              const SizedBox(width: 8),
              Text(
                label,
                style: TextStyle(
                  color: isPrimary ? Colors.black : Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTag(String text) {
    return Text(
      text,
      style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w500),
    );
  }

  Widget _buildDot() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 8),
      width: 3,
      height: 3,
      decoration: const BoxDecoration(color: Colors.grey, shape: BoxShape.circle),
    );
  }
}

class ContinueWatchingList extends StatelessWidget {
  const ContinueWatchingList({super.key});

  @override
  Widget build(BuildContext context) {
    final items = [
      {'title': 'Oppenheimer', 'image': 'https://image.tmdb.org/t/p/w500/8Gxv9mYjtUB1zYXYwqvUrQvxtWg.jpg', 'progress': 0.7},
      {'title': 'The Last of Us', 'image': 'https://image.tmdb.org/t/p/w500/uKvH569mi95m7un96Xsh99U8XjP.jpg', 'progress': 0.3},
    ];

    return SizedBox(
      height: 220,
      child: ListView.builder(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        scrollDirection: Axis.horizontal,
        itemCount: items.length,
        itemBuilder: (context, index) {
          final item = items[index];
          return Container(
            width: 160,
            margin: const EdgeInsets.only(right: 12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Stack(
                  alignment: Alignment.bottomCenter,
                  children: [
                    ClipRRect(
                      borderRadius: const BorderRadius.vertical(top: Radius.circular(4)),
                      child: Image.network(
                        item['image'] as String,
                        height: 180,
                        width: 160,
                        fit: BoxFit.cover,
                      ),
                    ),
                    // Progress Bar
                    Container(
                      height: 4,
                      width: 160,
                      color: Colors.grey[800],
                      alignment: Alignment.centerLeft,
                      child: Container(
                        width: 160 * (item['progress'] as double),
                        color: const Color(0xFFE50914),
                      ),
                    ),
                    // Play Overlay
                    Positioned.fill(
                      child: Center(
                        child: Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            border: Border.all(color: Colors.white, width: 2),
                            color: Colors.black26,
                          ),
                          child: const Icon(Icons.play_arrow, color: Colors.white, size: 30),
                        ),
                      ),
                    ),
                  ],
                ),
                Container(
                  color: const Color(0xFF121212),
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Icon(Icons.info_outline, color: Colors.grey, size: 20),
                      const Icon(Icons.more_vert, color: Colors.grey, size: 20),
                    ],
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class MovieCategoryList extends StatelessWidget {
  final List<String> posterUrls;

  const MovieCategoryList({super.key, required this.posterUrls});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 200,
      child: ListView.builder(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        scrollDirection: Axis.horizontal,
        itemCount: posterUrls.length,
        itemBuilder: (context, index) {
          return Container(
            width: 135,
            margin: const EdgeInsets.only(right: 10),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: Image.network(
                posterUrls[index],
                fit: BoxFit.cover,
              ),
            ),
          );
        },
      ),
    );
  }
}
