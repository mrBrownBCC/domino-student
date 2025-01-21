package bcc.snowman;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all
 * platforms.
 */
public class Main extends ApplicationAdapter {
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera camera;
    private ArrayList<Body> bodies;
    private ShapeRenderer shapeRenderer;
    private float WIDTH = 9;
    private float HEIGHT = 6;
    private float DOMINO_WIDTH = .2f;
    private float DOMINO_HEIGHT  =1f;

    private final float BOUNCINESS = .5f;
    private final float GRAVITY = 3f;

    public void create() {
        world = new World(new Vector2(0, -GRAVITY), true);
        debugRenderer = new Box2DDebugRenderer();
        shapeRenderer = new ShapeRenderer(); // Initialize ShapeRenderer

        // Set up the camera
        camera = new OrthographicCamera();
        camera.setToOrtho(false, WIDTH, HEIGHT);
        bodies = new ArrayList<Body>();
        addBorders();

        addBorders();
        addAllDominos();
        dominoStart();
    }

    public void addBorders() {
        createPlatform(WIDTH / 2, 0, WIDTH, .01f);// BOTTOM
        createPlatform(0, HEIGHT / 2, .01f, HEIGHT);// LEFT
        createPlatform(WIDTH, HEIGHT / 2, .01f, HEIGHT);// RIGHT
    }

    private void createPlatform(float centerX, float centerY, float width, float height) {
        // Create the body definition
        BodyDef wallBodyDef = new BodyDef();
        wallBodyDef.position.set(new Vector2(centerX, centerY));

        // Create the body in the world
        Body wallBody = world.createBody(wallBodyDef);

        // Create a polygon shape
        PolygonShape wallShape = new PolygonShape();
        wallShape.setAsBox(width / 2, height / 2);

        // Create a fixture for the shape and attach it to the body
        wallBody.createFixture(wallShape, 0.0f);

        // Dispose of the shape
        wallShape.dispose();
        bodies.add(wallBody);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        world.step(1 / 60f, 6, 2);
        camera.update();

        // Render the ground and physics bodies using the debug renderer
        debugRenderer.render(world, camera.combined);

        renderShapes(bodies);
    }

    private void renderShapes(ArrayList<Body> bodies) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1, 1, 1, 1); // Set color to white
        for (Body body : bodies) {
            Vector2 bodyPosition = body.getPosition(); // Position of the body
            float bodyAngle = body.getAngle(); // Rotation of the body
            for (Fixture fixture : body.getFixtureList()) {
                if (fixture.getShape() instanceof CircleShape) {
                    CircleShape circleShape = (CircleShape) fixture.getShape();

                    // Get the local position of the circle shape
                    Vector2 localPosition = circleShape.getPosition();
                    localPosition.rotateRad(bodyAngle); // Apply body rotation
                    localPosition.add(bodyPosition); // Add body position to get world position

                    // Render the circle
                    shapeRenderer.circle(localPosition.x, localPosition.y, circleShape.getRadius(), 30);
                } else if (fixture.getShape() instanceof PolygonShape) {//code generously provided by chatGPT
                    PolygonShape polygonShape = (PolygonShape) fixture.getShape();
                    int vertexCount = polygonShape.getVertexCount();
                    float angleDeg = body.getAngle() * MathUtils.radiansToDegrees;
                    if (vertexCount == 4) {
                        // Track min/max of local coordinates
                        float minX = Float.MAX_VALUE;
                        float maxX = Float.MIN_VALUE;
                        float minY = Float.MAX_VALUE;
                        float maxY = Float.MIN_VALUE;
    
                        Vector2 tmp = new Vector2();
    
                        // 1) Find local-space bounding box
                        for (int i = 0; i < vertexCount; i++) {
                            polygonShape.getVertex(i, tmp); // local coords
                            minX = Math.min(minX, tmp.x);
                            maxX = Math.max(maxX, tmp.x);
                            minY = Math.min(minY, tmp.y);
                            maxY = Math.max(maxY, tmp.y);
                        }
                        Vector2 bodyPos = body.getPosition();
                        // 2) Compute width and height in local space
                        float localWidth = maxX - minX;   // should be 2 * halfWidth
                        float localHeight = maxY - minY;  // should be 2 * halfHeight
    
                        // 3) Center of bounding box in local coordinates
                        float localCenterX = (minX + maxX) * 0.5f;
                        float localCenterY = (minY + maxY) * 0.5f;
    
                        // 4) Transform that center to world space
                        // Rotate local center, then translate by body pos
                        Vector2 worldCenter = new Vector2(localCenterX, localCenterY)
                                .rotateRad(angleDeg)
                                .add(bodyPos);
    
                        // 5) Compute bottom-left corner for shapeRenderer.rect(...)
                        float bottomLeftX = worldCenter.x - localWidth * 0.5f;
                        float bottomLeftY = worldCenter.y - localHeight * 0.5f;
    
                        // 6) Draw the rectangle in FILLED mode, rotating around its center
                        shapeRenderer.rect(
                                bottomLeftX,             // x (bottom-left corner)
                                bottomLeftY,             // y (bottom-left corner)
                                localWidth * 0.5f,       // originX (center relative to bottom-left)
                                localHeight * 0.5f,      // originY (center relative to bottom-left)
                                localWidth,
                                localHeight,
                                1f,                      // scaleX
                                1f,                      // scaleY
                                angleDeg             // rotation (degrees)
                        );
                    }
                }
            }
        }
        shapeRenderer.end();
    }

    private void addAllDominos() {
        //add your code here!!
    }

    // note - creates from center
    private void addDomino(float x, float y) {
        BodyDef bodyDef = new BodyDef();

        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);

        Body domino = world.createBody(bodyDef);

        PolygonShape dominoShape = new PolygonShape();
        dominoShape.setAsBox(DOMINO_WIDTH / 2f, DOMINO_HEIGHT / 2f);

        FixtureDef dominoFixture = new FixtureDef();
        dominoFixture.shape = dominoShape;
        dominoFixture.density = 1f;
        dominoFixture.friction = 0.5f;
        dominoFixture.restitution = BOUNCINESS;

        domino.createFixture(dominoFixture);

        // 6. Dispose shape when done
        dominoShape.dispose();

        bodies.add(domino);
    }

    private void dominoStart() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(.5f, .5f);
        bodyDef.linearVelocity.set(1.5f, 0f);

        Body ball = world.createBody(bodyDef);

        CircleShape ballShape = new CircleShape();
        ballShape.setRadius(.2f);

        FixtureDef ballFixture = new FixtureDef();
        ballFixture.shape = ballShape;
        ballFixture.density = 1f;
        ballFixture.restitution = BOUNCINESS; // Make the ball bouncy

        ball.createFixture(ballFixture);

        ballShape.dispose();
        bodies.add(ball);
    }

    @Override
    public void dispose() {
        world.dispose();
        debugRenderer.dispose();
        shapeRenderer.dispose(); // Dispose of ShapeRenderer
    }
}
